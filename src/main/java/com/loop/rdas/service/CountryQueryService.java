package com.loop.rdas.service;

import com.loop.rdas.config.CacheConfig;
import com.loop.rdas.dto.CatalogStatusResponse;
import com.loop.rdas.dto.CountryDetailResponse;
import com.loop.rdas.dto.CountrySearchRequest;
import com.loop.rdas.dto.CountrySummaryResponse;
import com.loop.rdas.dto.PagedResponse;
import com.loop.rdas.dto.ReferenceItemResponse;
import com.loop.rdas.exception.CountryNotFoundException;
import com.loop.rdas.model.Country;
import com.loop.rdas.model.ReferenceType;
import com.loop.rdas.repository.CountryRepository;
import com.loop.rdas.repository.ReferenceItemRepository;
import com.loop.rdas.spec.CountrySpecifications;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-side service for the reference-data catalog. Every method is served
 * entirely from the local database and the in-process cache — no SOAP calls
 * occur on this path.
 */
@Service
@Transactional(readOnly = true)
public class CountryQueryService {

    /** Sort properties a client is permitted to sort by. */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "name", "isoCode", "capitalCity", "continentCode", "continentName",
            "currencyCode", "currencyName");

    private final CountryRepository countryRepository;
    private final ReferenceItemRepository referenceRepository;
    private final CatalogStatus catalogStatus;

    @Value("${rdas.harvest.stale-after-hours:24}")
    private long staleAfterHours;

    public CountryQueryService(CountryRepository countryRepository,
                               ReferenceItemRepository referenceRepository,
                               CatalogStatus catalogStatus) {
        this.countryRepository = countryRepository;
        this.referenceRepository = referenceRepository;
        this.catalogStatus = catalogStatus;
    }

    @Cacheable(cacheNames = CacheConfig.COUNTRY_SEARCH, key =
            "T(java.util.Objects).toString(#request.name) + '|' + #request.continent + '|' "
            + "+ #request.currency + '|' + #request.language + '|' + #pageable.pageNumber + '|' "
            + "+ #pageable.pageSize + '|' + #pageable.sort.toString()")
    public PagedResponse<CountrySummaryResponse> search(CountrySearchRequest request, Pageable pageable) {
        validateSort(pageable.getSort());
        Page<Country> page = countryRepository.findAll(
                CountrySpecifications.fromRequest(request), pageable);
        return PagedResponse.from(page, CountrySummaryResponse::from);
    }

    @Cacheable(cacheNames = CacheConfig.COUNTRY_BY_ISO, key = "#isoCode.toUpperCase()")
    public CountryDetailResponse getByIsoCode(String isoCode) {
        Country country = countryRepository.findByIsoCodeIgnoreCase(isoCode)
                .orElseThrow(() -> new CountryNotFoundException(
                        "No country found for ISO code '" + isoCode + "'"));
        return CountryDetailResponse.from(country);
    }

    @Cacheable(cacheNames = CacheConfig.CURRENCY_PEERS, key = "#isoCode.toUpperCase()")
    public List<CountrySummaryResponse> getCountriesSharingCurrency(String isoCode) {
        Country country = countryRepository.findByIsoCodeIgnoreCase(isoCode)
                .orElseThrow(() -> new CountryNotFoundException(
                        "No country found for ISO code '" + isoCode + "'"));
        if (country.getCurrencyCode() == null) {
            return List.of();
        }
        return countryRepository
                .findByCurrencyCodeIgnoreCaseOrderByNameAsc(country.getCurrencyCode())
                .stream()
                .map(CountrySummaryResponse::from)
                .toList();
    }

    @Cacheable(cacheNames = CacheConfig.REFERENCE_LIST, key = "#type.name()")
    public List<ReferenceItemResponse> getReferenceItems(ReferenceType type) {
        return referenceRepository.findByTypeOrderByNameAsc(type).stream()
                .map(ReferenceItemResponse::from)
                .toList();
    }

    public CatalogStatusResponse getCatalogStatus() {
        long countries = countryRepository.count();
        Instant latest = countryRepository.findLatestRefreshedAt();
        boolean stale = latest == null
                || Duration.between(latest, Instant.now()).toHours() >= staleAfterHours;
        return new CatalogStatusResponse(
                countries,
                referenceRepository.countByType(ReferenceType.CONTINENT),
                referenceRepository.countByType(ReferenceType.CURRENCY),
                referenceRepository.countByType(ReferenceType.LANGUAGE),
                catalogStatus.getLastStartedAt(),
                catalogStatus.getLastCompletedAt(),
                catalogStatus.isLastSuccessful(),
                catalogStatus.getLastError(),
                catalogStatus.isRefreshInProgress(),
                stale);
    }

    private void validateSort(Sort sort) {
        for (Sort.Order order : sort) {
            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                throw new IllegalArgumentException("Invalid sort property '" + order.getProperty()
                        + "'. Allowed: " + ALLOWED_SORT_FIELDS);
            }
        }
    }
}
