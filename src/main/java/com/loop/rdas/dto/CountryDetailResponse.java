package com.loop.rdas.dto;

import com.loop.rdas.model.Country;
import java.time.Instant;
import java.util.List;

/**
 * Full country detail returned by {@code GET /api/v1/countries/{isoCode}}.
 */
public record CountryDetailResponse(
        String isoCode,
        String name,
        String capitalCity,
        String phoneCode,
        String continentCode,
        String continentName,
        String currencyCode,
        String currencyName,
        String flagUrl,
        List<LanguageDto> languages,
        Instant sourceRefreshedAt) {

    public static CountryDetailResponse from(Country c) {
        return new CountryDetailResponse(
                c.getIsoCode(),
                c.getName(),
                c.getCapitalCity(),
                c.getPhoneCode(),
                c.getContinentCode(),
                c.getContinentName(),
                c.getCurrencyCode(),
                c.getCurrencyName(),
                c.getFlagUrl(),
                c.getLanguages().stream().map(LanguageDto::from).toList(),
                c.getSourceRefreshedAt());
    }
}
