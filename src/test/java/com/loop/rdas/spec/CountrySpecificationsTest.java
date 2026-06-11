package com.loop.rdas.spec;

import static org.assertj.core.api.Assertions.assertThat;

import com.loop.rdas.dto.CountrySearchRequest;
import com.loop.rdas.model.Country;
import com.loop.rdas.model.CountryLanguage;
import com.loop.rdas.repository.CountryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@DataJpaTest
class CountrySpecificationsTest {

    @Autowired
    CountryRepository repository;

    @BeforeEach
    void seed() {
        repository.deleteAll();
        repository.save(country("KE", "Kenya", "AF", "Africa", "KES", "Kenyan Shilling", "swa", "Swahili"));
        repository.save(country("TZ", "Tanzania", "AF", "Africa", "TZS", "Tanzanian Shilling", "swa", "Swahili"));
        repository.save(country("US", "United States", "AM", "The Americas", "USD", "US Dollar", "eng", "English"));
        repository.save(country("EC", "Ecuador", "AM", "The Americas", "USD", "US Dollar", "spa", "Spanish"));
    }

    private Country country(String iso, String name, String contCode, String contName,
                           String curCode, String curName, String langCode, String langName) {
        Country c = new Country();
        c.setIsoCode(iso);
        c.setName(name);
        c.setContinentCode(contCode);
        c.setContinentName(contName);
        c.setCurrencyCode(curCode);
        c.setCurrencyName(curName);
        c.getLanguages().add(new CountryLanguage(langCode, langName));
        c.setSourceRefreshedAt(java.time.Instant.now());
        return c;
    }

    private CountrySearchRequest request(String name, String continent, String currency, String language) {
        CountrySearchRequest r = new CountrySearchRequest();
        r.setName(name);
        r.setContinent(continent);
        r.setCurrency(currency);
        r.setLanguage(language);
        return r;
    }

    private long count(CountrySearchRequest r) {
        return repository.findAll(CountrySpecifications.fromRequest(r),
                PageRequest.of(0, 50, Sort.by("name"))).getTotalElements();
    }

    @Test
    void noFilters_returnsAll() {
        assertThat(count(request(null, null, null, null))).isEqualTo(4);
    }

    @Test
    void filterByNamePartialCaseInsensitive() {
        assertThat(count(request("ken", null, null, null))).isEqualTo(1);
        assertThat(count(request("tan", null, null, null))).isEqualTo(1); // Tanzania
    }

    @Test
    void filterByContinentCodeOrName() {
        assertThat(count(request(null, "AF", null, null))).isEqualTo(2);
        assertThat(count(request(null, "Americas", null, null))).isEqualTo(2);
    }

    @Test
    void filterByCurrency() {
        assertThat(count(request(null, null, "USD", null))).isEqualTo(2);
        assertThat(count(request(null, null, "KES", null))).isEqualTo(1);
    }

    @Test
    void filterByLanguageCodeOrName() {
        assertThat(count(request(null, null, null, "swa"))).isEqualTo(2);
        assertThat(count(request(null, null, null, "English"))).isEqualTo(1);
    }

    @Test
    void combinedFilters_andSemantics() {
        assertThat(count(request(null, "AF", "KES", "swa"))).isEqualTo(1);
        assertThat(count(request(null, "AM", "KES", null))).isEqualTo(0);
    }

    @Test
    void currencyPeers_query() {
        assertThat(repository.findByCurrencyCodeIgnoreCaseOrderByNameAsc("USD")).hasSize(2);
    }
}
