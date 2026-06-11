package com.loop.rdas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.loop.rdas.client.CountryInfoSoapClient;
import com.loop.rdas.exception.SoapIntegrationException;
import com.loop.rdas.model.Country;
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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CatalogHarvestServiceTest {

    @Mock
    CountryInfoSoapClient soapClient;
    @Mock
    CountryRepository countryRepository;
    @Mock
    ReferenceItemRepository referenceRepository;
    @Mock
    CacheManager cacheManager;
    @Mock
    Cache cache;

    CatalogStatus status;
    CatalogHarvestService service;

    @BeforeEach
    void setUp() {
        status = new CatalogStatus();
        service = new CatalogHarvestService(soapClient, countryRepository, referenceRepository,
                status, cacheManager);
        ReflectionTestUtils.setField(service, "requestDelayMs", 0L);
        lenient().when(cacheManager.getCache(anyString())).thenReturn(cache);
    }

    private TContinent continent(String code, String name) {
        TContinent c = new TContinent();
        c.setCode(code);
        c.setName(name);
        return c;
    }

    private TCurrency currency(String code, String name) {
        TCurrency c = new TCurrency();
        c.setIsoCode(code);
        c.setName(name);
        return c;
    }

    private TLanguage language(String code, String name) {
        TLanguage l = new TLanguage();
        l.setIsoCode(code);
        l.setName(name);
        return l;
    }

    private TCountryCodeAndNameGroupedByContinent group(String continentCode, String continentName,
                                                        String iso, String name) {
        TCountryCodeAndNameGroupedByContinent g = new TCountryCodeAndNameGroupedByContinent();
        g.setContinent(continent(continentCode, continentName));
        TCountryCodeAndName c = new TCountryCodeAndName();
        c.setIsoCode(iso);
        c.setName(name);
        g.setCountries(List.of(c));
        return g;
    }

    private TCountryInfo fullInfo(String iso, String name, String continentCode, String currency) {
        TCountryInfo info = new TCountryInfo();
        info.setIsoCode(iso);
        info.setName(name);
        info.setCapitalCity(name + " City");
        info.setPhoneCode("254");
        info.setContinentCode(continentCode);
        info.setCurrencyISOCode(currency);
        info.setCountryFlag("http://flags/" + iso + ".jpg");
        info.getLanguages().add(language("swa", "Swahili"));
        return info;
    }

    @Test
    void refresh_harvestsReferenceListsAndCountries() {
        when(soapClient.listContinents()).thenReturn(List.of(continent("AF", "Africa   ")));
        when(soapClient.listCurrencies()).thenReturn(List.of(currency("KES", "Kenyan Shilling")));
        when(soapClient.listLanguages()).thenReturn(List.of(language("swa", "Swahili")));
        when(soapClient.listCountriesGroupedByContinent())
                .thenReturn(List.of(group("AF", "Africa   ", "KE", "Kenya")));
        when(soapClient.getFullCountryInfo("KE")).thenReturn(fullInfo("KE", "Kenya", "AF", "KES"));
        when(countryRepository.findByIsoCodeIgnoreCase("KE")).thenReturn(Optional.empty());
        when(referenceRepository.findByTypeAndCodeIgnoreCase(any(), anyString()))
                .thenReturn(Optional.empty());

        HarvestSummary summary = service.refresh();

        assertThat(summary.successful()).isTrue();
        assertThat(summary.countriesProcessed()).isEqualTo(1);
        assertThat(summary.continents()).isEqualTo(1);
        assertThat(summary.currencies()).isEqualTo(1);

        ArgumentCaptor<Country> captor = ArgumentCaptor.forClass(Country.class);
        verify(countryRepository).save(captor.capture());
        Country saved = captor.getValue();
        assertThat(saved.getIsoCode()).isEqualTo("KE");
        assertThat(saved.getContinentName()).isEqualTo("Africa"); // trimmed
        assertThat(saved.getCurrencyName()).isEqualTo("Kenyan Shilling");
        assertThat(saved.getLanguages()).hasSize(1);

        // Reference items persisted (1 continent + 1 currency + 1 language).
        ArgumentCaptor<ReferenceItem> refCaptor = ArgumentCaptor.forClass(ReferenceItem.class);
        verify(referenceRepository, org.mockito.Mockito.times(3)).save(refCaptor.capture());
        assertThat(refCaptor.getAllValues()).extracting(ReferenceItem::getType)
                .contains(ReferenceType.CONTINENT, ReferenceType.CURRENCY, ReferenceType.LANGUAGE);

        // Status reflects success, caches evicted.
        assertThat(status.isLastSuccessful()).isTrue();
        verify(cache, org.mockito.Mockito.atLeastOnce()).clear();
    }

    @Test
    void refresh_failsGracefullyWhenSoapDown() {
        when(soapClient.listContinents()).thenThrow(new SoapIntegrationException("SOAP down"));

        HarvestSummary summary = service.refresh();

        assertThat(summary.started()).isTrue();
        assertThat(summary.successful()).isFalse();
        assertThat(status.isLastSuccessful()).isFalse();
        assertThat(status.getLastError()).contains("SOAP down");
        verify(countryRepository, never()).save(any());
    }

    @Test
    void refresh_isSingleFlight() {
        status.beginRefresh(); // simulate an in-progress harvest
        HarvestSummary summary = service.refresh();
        assertThat(summary.started()).isFalse();
        assertThat(summary.message()).contains("already in progress");
        verify(soapClient, never()).listContinents();
    }

    @Test
    void refresh_continuesWhenOneCountryFails() {
        when(soapClient.listContinents()).thenReturn(List.of(continent("AF", "Africa")));
        when(soapClient.listCurrencies()).thenReturn(List.of(currency("KES", "Kenyan Shilling")));
        when(soapClient.listLanguages()).thenReturn(List.of());
        when(soapClient.listCountriesGroupedByContinent()).thenReturn(List.of(
                group("AF", "Africa", "KE", "Kenya"),
                group("AF", "Africa", "ZZ", "Brokenland")));
        when(soapClient.getFullCountryInfo("KE")).thenReturn(fullInfo("KE", "Kenya", "AF", "KES"));
        when(soapClient.getFullCountryInfo("ZZ"))
                .thenThrow(new SoapIntegrationException("not found"));
        when(countryRepository.findByIsoCodeIgnoreCase(eq("KE"))).thenReturn(Optional.empty());
        when(referenceRepository.findByTypeAndCodeIgnoreCase(any(), anyString()))
                .thenReturn(Optional.empty());

        HarvestSummary summary = service.refresh();

        assertThat(summary.successful()).isTrue();
        assertThat(summary.countriesProcessed()).isEqualTo(1);
        assertThat(summary.countriesFailed()).isEqualTo(1);
    }
}
