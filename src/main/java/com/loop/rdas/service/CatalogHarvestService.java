package com.loop.rdas.service;

import com.loop.rdas.client.CountryInfoSoapClient;
import com.loop.rdas.config.CacheConfig;
import com.loop.rdas.model.Country;
import com.loop.rdas.model.CountryLanguage;
import com.loop.rdas.model.ReferenceItem;
import com.loop.rdas.model.ReferenceType;
import com.loop.rdas.repository.CountryRepository;
import com.loop.rdas.repository.ReferenceItemRepository;
import com.loop.rdas.soap.TContinent;
import com.loop.rdas.soap.TCountryCodeAndName;
import com.loop.rdas.soap.TCountryCodeAndNameGroupedByContinent;
import com.loop.rdas.soap.TCountryInfo;
import com.loop.rdas.soap.TCurrency;
import com.loop.rdas.soap.TLanguage;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Builds and refreshes the materialized country catalog from the CountryInfo
 * SOAP service. This is the heart of the SOAP-traffic-reduction strategy: a
 * full harvest runs on a schedule (and once after startup if the catalog is
 * empty or stale), while every read API serves from the catalog with zero SOAP
 * calls.
 *
 * <p>A harvest performs four read-only list calls plus one
 * {@code FullCountryInfo} call per country (~240), all paced and rate-limited
 * to stay under the provider's 100 requests/minute quota. Countries are
 * upserted incrementally, so an upstream failure part-way through leaves the
 * previously harvested data intact and serving.</p>
 */
@Service
public class CatalogHarvestService {

    private static final Logger log = LoggerFactory.getLogger(CatalogHarvestService.class);

    private final CountryInfoSoapClient soapClient;
    private final CountryRepository countryRepository;
    private final ReferenceItemRepository referenceRepository;
    private final CatalogStatus status;
    private final CacheManager cacheManager;

    @Value("${rdas.harvest.request-delay-ms:120}")
    private long requestDelayMs;

    public CatalogHarvestService(CountryInfoSoapClient soapClient,
                                 CountryRepository countryRepository,
                                 ReferenceItemRepository referenceRepository,
                                 CatalogStatus status,
                                 CacheManager cacheManager) {
        this.soapClient = soapClient;
        this.countryRepository = countryRepository;
        this.referenceRepository = referenceRepository;
        this.status = status;
        this.cacheManager = cacheManager;
    }

    /** Scheduled full refresh (cron from {@code rdas.harvest.cron}). */
    @Scheduled(cron = "${rdas.harvest.cron:0 15 2 * * *}")
    public void scheduledRefresh() {
        log.info("Scheduled catalog refresh triggered");
        refresh();
    }

    /** Fire-and-forget refresh used by the startup runner and the admin endpoint. */
    @Async("harvestExecutor")
    public void refreshAsync() {
        refresh();
    }

    /**
     * Runs a full catalog harvest. Single-flight: concurrent invocations return
     * immediately with {@link HarvestSummary#alreadyRunning()}.
     */
    public HarvestSummary refresh() {
        if (!status.beginRefresh()) {
            log.info("Catalog refresh requested but one is already in progress");
            return HarvestSummary.alreadyRunning();
        }
        long start = System.currentTimeMillis();
        int processed = 0;
        int failed = 0;
        int continents = 0;
        int currencies = 0;
        int languages = 0;
        try {
            // 1. Reference lists (also used to resolve names while enriching).
            Map<String, String> continentNames = harvestContinents();
            continents = continentNames.size();
            Map<String, String> currencyNames = harvestCurrencies();
            currencies = currencyNames.size();
            languages = harvestLanguages();

            // 2. Seed the universe of countries (ISO code + name + continent).
            List<TCountryCodeAndNameGroupedByContinent> groups =
                    soapClient.listCountriesGroupedByContinent();

            // 3. Enrich each country and upsert it.
            for (TCountryCodeAndNameGroupedByContinent group : groups) {
                String groupContinentCode = group.getContinent() != null
                        ? trim(group.getContinent().getCode()) : null;
                for (TCountryCodeAndName seed : safe(group.getCountries())) {
                    try {
                        upsertCountry(seed, groupContinentCode, continentNames, currencyNames);
                        processed++;
                    } catch (Exception e) {
                        failed++;
                        log.warn("Failed to harvest country '{}' ({}): {}",
                                seed.getName(), seed.getIsoCode(), e.getMessage());
                    }
                    pace();
                }
            }

            status.completeSuccess();
            evictReadCaches();
            long duration = System.currentTimeMillis() - start;
            log.info("Catalog harvest complete: {} countries ({} failed), {} continents, "
                            + "{} currencies, {} languages in {} ms",
                    processed, failed, continents, currencies, languages, duration);
            return new HarvestSummary(true, true, processed, failed, continents, currencies,
                    languages, duration, "Harvest complete");
        } catch (Exception e) {
            status.completeFailure(e.getMessage());
            long duration = System.currentTimeMillis() - start;
            log.error("Catalog harvest failed after {} ms: {}", duration, e.getMessage());
            return new HarvestSummary(true, false, processed, failed, continents, currencies,
                    languages, duration, "Harvest failed: " + e.getMessage());
        }
    }

    private Map<String, String> harvestContinents() {
        Map<String, String> byCode = new HashMap<>();
        for (TContinent c : soapClient.listContinents()) {
            String code = trim(c.getCode());
            String name = trim(c.getName());
            if (StringUtils.hasText(code)) {
                byCode.put(code.toUpperCase(), name);
                upsertReference(ReferenceType.CONTINENT, code, name);
            }
        }
        return byCode;
    }

    private Map<String, String> harvestCurrencies() {
        Map<String, String> byCode = new HashMap<>();
        for (TCurrency c : soapClient.listCurrencies()) {
            String code = trim(c.getIsoCode());
            String name = trim(c.getName());
            if (StringUtils.hasText(code)) {
                byCode.put(code.toUpperCase(), name);
                upsertReference(ReferenceType.CURRENCY, code, name);
            }
        }
        return byCode;
    }

    private int harvestLanguages() {
        int count = 0;
        for (TLanguage l : soapClient.listLanguages()) {
            String code = trim(l.getIsoCode());
            String name = trim(l.getName());
            if (StringUtils.hasText(code)) {
                upsertReference(ReferenceType.LANGUAGE, code, name);
                count++;
            }
        }
        return count;
    }

    private void upsertCountry(TCountryCodeAndName seed,
                               String groupContinentCode,
                               Map<String, String> continentNames,
                               Map<String, String> currencyNames) {
        String iso = trim(seed.getIsoCode());
        if (!StringUtils.hasText(iso)) {
            return;
        }
        TCountryInfo full = soapClient.getFullCountryInfo(iso);

        Country country = countryRepository.findByIsoCodeIgnoreCase(iso)
                .orElseGet(Country::new);
        country.setIsoCode(iso.toUpperCase());
        country.setName(firstNonBlank(trim(full.getName()), trim(seed.getName())));
        country.setCapitalCity(trim(full.getCapitalCity()));
        country.setPhoneCode(trim(full.getPhoneCode()));

        String continentCode = firstNonBlank(trim(full.getContinentCode()), groupContinentCode);
        country.setContinentCode(continentCode != null ? continentCode.toUpperCase() : null);
        country.setContinentName(continentCode != null
                ? continentNames.get(continentCode.toUpperCase()) : null);

        String currencyCode = trim(full.getCurrencyISOCode());
        country.setCurrencyCode(StringUtils.hasText(currencyCode) ? currencyCode.toUpperCase() : null);
        country.setCurrencyName(StringUtils.hasText(currencyCode)
                ? currencyNames.get(currencyCode.toUpperCase()) : null);

        country.setFlagUrl(trim(full.getCountryFlag()));

        Set<CountryLanguage> langs = new LinkedHashSet<>();
        for (TLanguage l : safe(full.getLanguages())) {
            if (StringUtils.hasText(l.getIsoCode())) {
                langs.add(new CountryLanguage(trim(l.getIsoCode()), trim(l.getName())));
            }
        }
        country.replaceLanguages(langs);
        country.setSourceRefreshedAt(java.time.Instant.now());

        countryRepository.save(country);
    }

    private void upsertReference(ReferenceType type, String code, String name) {
        ReferenceItem item = referenceRepository.findByTypeAndCodeIgnoreCase(type, code)
                .orElseGet(ReferenceItem::new);
        item.setType(type);
        item.setCode(code);
        item.setName(name);
        referenceRepository.save(item);
    }

    private void evictReadCaches() {
        clear(CacheConfig.COUNTRY_SEARCH);
        clear(CacheConfig.COUNTRY_BY_ISO);
        clear(CacheConfig.CURRENCY_PEERS);
        clear(CacheConfig.REFERENCE_LIST);
    }

    private void clear(String cacheName) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    private void pace() {
        if (requestDelayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(requestDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static <T> List<T> safe(List<T> list) {
        return list != null ? list : List.of();
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private static String firstNonBlank(String a, String b) {
        return StringUtils.hasText(a) ? a : b;
    }
}
