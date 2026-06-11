package com.loop.rdas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.loop.rdas.dto.CatalogStatusResponse;
import com.loop.rdas.dto.CountrySearchRequest;
import com.loop.rdas.exception.CountryNotFoundException;
import com.loop.rdas.model.Country;
import com.loop.rdas.model.CountryLanguage;
import com.loop.rdas.model.ReferenceItem;
import com.loop.rdas.model.ReferenceType;
import com.loop.rdas.repository.CountryRepository;
import com.loop.rdas.repository.ReferenceItemRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CountryQueryServiceTest {

    @Mock
    CountryRepository countryRepository;
    @Mock
    ReferenceItemRepository referenceRepository;

    CatalogStatus catalogStatus;
    CountryQueryService service;

    @BeforeEach
    void setUp() {
        catalogStatus = new CatalogStatus();
        service = new CountryQueryService(countryRepository, referenceRepository, catalogStatus);
        ReflectionTestUtils.setField(service, "staleAfterHours", 24L);
    }

    private Country kenya() {
        Country c = new Country();
        c.setIsoCode("KE");
        c.setName("Kenya");
        c.setCapitalCity("Nairobi");
        c.setContinentCode("AF");
        c.setContinentName("Africa");
        c.setCurrencyCode("KES");
        c.setCurrencyName("Kenyan Shilling");
        c.setFlagUrl("http://flags/KE.jpg");
        c.getLanguages().add(new CountryLanguage("swa", "Swahili"));
        c.setSourceRefreshedAt(Instant.now());
        return c;
    }

    @Test
    void search_mapsPageToDtoEnvelope() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("name"));
        Page<Country> page = new PageImpl<>(List.of(kenya()), pageable, 1);
        when(countryRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        var result = service.search(new CountrySearchRequest(), pageable);

        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).isoCode()).isEqualTo("KE");
        assertThat(result.first()).isTrue();
        assertThat(result.last()).isTrue();
    }

    @Test
    void search_rejectsUnknownSortProperty() {
        Pageable bad = PageRequest.of(0, 20, Sort.by("dropTable"));
        assertThatThrownBy(() -> service.search(new CountrySearchRequest(), bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid sort property");
    }

    @Test
    void getByIsoCode_returnsDetail() {
        when(countryRepository.findByIsoCodeIgnoreCase("KE")).thenReturn(Optional.of(kenya()));
        var detail = service.getByIsoCode("KE");
        assertThat(detail.name()).isEqualTo("Kenya");
        assertThat(detail.languages()).hasSize(1);
    }

    @Test
    void getByIsoCode_throwsWhenMissing() {
        when(countryRepository.findByIsoCodeIgnoreCase("ZZ")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getByIsoCode("ZZ"))
                .isInstanceOf(CountryNotFoundException.class);
    }

    @Test
    void getCountriesSharingCurrency_returnsPeers() {
        when(countryRepository.findByIsoCodeIgnoreCase("KE")).thenReturn(Optional.of(kenya()));
        when(countryRepository.findByCurrencyCodeIgnoreCaseOrderByNameAsc("KES"))
                .thenReturn(List.of(kenya()));
        var peers = service.getCountriesSharingCurrency("KE");
        assertThat(peers).hasSize(1);
    }

    @Test
    void getCountriesSharingCurrency_emptyWhenNoCurrency() {
        Country c = kenya();
        c.setCurrencyCode(null);
        when(countryRepository.findByIsoCodeIgnoreCase("KE")).thenReturn(Optional.of(c));
        assertThat(service.getCountriesSharingCurrency("KE")).isEmpty();
    }

    @Test
    void getReferenceItems_mapsItems() {
        ReferenceItem item = new ReferenceItem();
        item.setType(ReferenceType.CONTINENT);
        item.setCode("AF");
        item.setName("Africa");
        when(referenceRepository.findByTypeOrderByNameAsc(ReferenceType.CONTINENT))
                .thenReturn(List.of(item));
        var items = service.getReferenceItems(ReferenceType.CONTINENT);
        assertThat(items).extracting(r -> r.code()).containsExactly("AF");
    }

    @Test
    void getCatalogStatus_flagsStaleWhenOld() {
        when(countryRepository.count()).thenReturn(5L);
        when(countryRepository.findLatestRefreshedAt())
                .thenReturn(Instant.now().minusSeconds(60 * 60 * 48));
        when(referenceRepository.countByType(any())).thenReturn(3L);

        CatalogStatusResponse status = service.getCatalogStatus();
        assertThat(status.countryCount()).isEqualTo(5L);
        assertThat(status.stale()).isTrue();
    }

    @Test
    void getCatalogStatus_freshWhenRecent() {
        when(countryRepository.count()).thenReturn(5L);
        when(countryRepository.findLatestRefreshedAt()).thenReturn(Instant.now());
        when(referenceRepository.countByType(any())).thenReturn(3L);

        assertThat(service.getCatalogStatus().stale()).isFalse();
    }
}
