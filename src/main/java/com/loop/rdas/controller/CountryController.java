package com.loop.rdas.controller;

import com.loop.rdas.dto.CountryDetailResponse;
import com.loop.rdas.dto.CountrySearchRequest;
import com.loop.rdas.dto.CountrySummaryResponse;
import com.loop.rdas.dto.PagedResponse;
import com.loop.rdas.service.CountryQueryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Country search and lookup APIs. Backed entirely by the materialized catalog.
 *
 * <ul>
 *   <li>{@code GET /api/v1/countries} — search by name and filter by continent,
 *       currency and language, with pagination and sorting.</li>
 *   <li>{@code GET /api/v1/countries/{isoCode}} — full country detail.</li>
 *   <li>{@code GET /api/v1/countries/{isoCode}/currency-peers} — countries that
 *       share the given country's currency.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/countries")
@Validated
public class CountryController {

    private static final String ISO_PATTERN = "(?i)[a-z]{2,3}";

    private final CountryQueryService queryService;

    public CountryController(CountryQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    public PagedResponse<CountrySummaryResponse> searchCountries(
            @Valid CountrySearchRequest filters,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return queryService.search(filters, pageable);
    }

    @GetMapping("/{isoCode}")
    public CountryDetailResponse getCountry(
            @PathVariable @Pattern(regexp = ISO_PATTERN,
                    message = "isoCode must be a 2-3 letter country code") String isoCode) {
        return queryService.getByIsoCode(isoCode);
    }

    @GetMapping("/{isoCode}/currency-peers")
    public ResponseEntity<List<CountrySummaryResponse>> getCurrencyPeers(
            @PathVariable @Pattern(regexp = ISO_PATTERN,
                    message = "isoCode must be a 2-3 letter country code") String isoCode) {
        return ResponseEntity.ok(queryService.getCountriesSharingCurrency(isoCode));
    }
}
