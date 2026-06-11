package com.loop.rdas.dto;

import com.loop.rdas.model.Country;

/**
 * Lightweight country projection used in list/search results.
 */
public record CountrySummaryResponse(
        String isoCode,
        String name,
        String capitalCity,
        String continentCode,
        String continentName,
        String currencyCode,
        String currencyName,
        String flagUrl) {

    public static CountrySummaryResponse from(Country c) {
        return new CountrySummaryResponse(
                c.getIsoCode(),
                c.getName(),
                c.getCapitalCity(),
                c.getContinentCode(),
                c.getContinentName(),
                c.getCurrencyCode(),
                c.getCurrencyName(),
                c.getFlagUrl());
    }
}
