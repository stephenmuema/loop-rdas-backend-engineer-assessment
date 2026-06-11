package com.loop.rdas.controller;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.loop.rdas.model.Country;
import com.loop.rdas.model.CountryLanguage;
import com.loop.rdas.model.ReferenceItem;
import com.loop.rdas.model.ReferenceType;
import com.loop.rdas.repository.CountryRepository;
import com.loop.rdas.repository.ReferenceItemRepository;
import com.loop.rdas.service.CatalogHarvestService;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CountryApiIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    CountryRepository countryRepository;
    @Autowired
    ReferenceItemRepository referenceRepository;
    @Autowired
    CacheManager cacheManager;

    // Prevent any real SOAP harvest from being triggered during the test.
    @MockBean
    CatalogHarvestService harvestService;

    @BeforeEach
    void seed() {
        cacheManager.getCacheNames().forEach(n -> cacheManager.getCache(n).clear());
        countryRepository.deleteAll();
        referenceRepository.deleteAll();

        countryRepository.save(country("KE", "Kenya", "AF", "Africa", "KES", "Kenyan Shilling", "swa", "Swahili"));
        countryRepository.save(country("TZ", "Tanzania", "AF", "Africa", "TZS", "Tanzanian Shilling", "swa", "Swahili"));
        countryRepository.save(country("US", "United States", "AM", "The Americas", "USD", "US Dollar", "eng", "English"));
        countryRepository.save(country("EC", "Ecuador", "AM", "The Americas", "USD", "US Dollar", "spa", "Spanish"));

        referenceRepository.save(reference(ReferenceType.CONTINENT, "AF", "Africa"));
        referenceRepository.save(reference(ReferenceType.CURRENCY, "USD", "US Dollar"));
        referenceRepository.save(reference(ReferenceType.LANGUAGE, "swa", "Swahili"));
    }

    private Country country(String iso, String name, String contCode, String contName,
                           String curCode, String curName, String langCode, String langName) {
        Country c = new Country();
        c.setIsoCode(iso);
        c.setName(name);
        c.setCapitalCity(name + " City");
        c.setContinentCode(contCode);
        c.setContinentName(contName);
        c.setCurrencyCode(curCode);
        c.setCurrencyName(curName);
        c.setFlagUrl("http://flags/" + iso + ".jpg");
        c.getLanguages().add(new CountryLanguage(langCode, langName));
        c.setSourceRefreshedAt(Instant.now());
        return c;
    }

    private ReferenceItem reference(ReferenceType type, String code, String name) {
        ReferenceItem item = new ReferenceItem();
        item.setType(type);
        item.setCode(code);
        item.setName(name);
        return item;
    }

    @Test
    void listCountries_returnsPagedEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/countries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void listCountries_paginationAndSort() throws Exception {
        mockMvc.perform(get("/api/v1/countries?page=0&size=2&sort=name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].isoCode").value("EC")); // Ecuador first by name
    }

    @Test
    void filterByContinent() throws Exception {
        mockMvc.perform(get("/api/v1/countries?continent=AF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void filterByCurrencyAndLanguage() throws Exception {
        mockMvc.perform(get("/api/v1/countries?currency=USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
        mockMvc.perform(get("/api/v1/countries?language=Swahili"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void searchByName() throws Exception {
        mockMvc.perform(get("/api/v1/countries?name=ken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].isoCode").value("KE"));
    }

    @Test
    void invalidSort_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/countries?sort=dropTable,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid sort")));
    }

    @Test
    void invalidFilterTooLong_returns400() throws Exception {
        String longName = "x".repeat(200);
        mockMvc.perform(get("/api/v1/countries?name=" + longName))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCountryByIso_returnsDetail() throws Exception {
        mockMvc.perform(get("/api/v1/countries/KE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isoCode").value("KE"))
                .andExpect(jsonPath("$.name").value("Kenya"))
                .andExpect(jsonPath("$.languages[0].code").value("swa"));
    }

    @Test
    void getCountryByIso_unknown_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/countries/ZZ"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getCountryByIso_badPattern_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/countries/12"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void currencyPeers_returnsSharers() throws Exception {
        mockMvc.perform(get("/api/v1/countries/US/currency-peers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2)); // US + Ecuador share USD
    }

    @Test
    void referenceLists_returnData() throws Exception {
        mockMvc.perform(get("/api/v1/continents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("AF"));
        mockMvc.perform(get("/api/v1/currencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("USD"));
        mockMvc.perform(get("/api/v1/languages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("swa"));
    }

    @Test
    void catalogStatus_returnsCounts() throws Exception {
        mockMvc.perform(get("/api/v1/admin/catalog/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countryCount").value(4));
    }

    @Test
    void refresh_triggersHarvestAndReturns202() throws Exception {
        mockMvc.perform(post("/api/v1/admin/catalog/refresh"))
                .andExpect(status().isAccepted());
        verify(harvestService).refreshAsync();
    }
}
