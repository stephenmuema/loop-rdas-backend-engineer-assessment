package com.loop.rdas.dto;

import com.loop.rdas.model.CountryLanguage;

/**
 * A language spoken in a country.
 */
public record LanguageDto(String code, String name) {

    public static LanguageDto from(CountryLanguage l) {
        return new LanguageDto(l.getCode(), l.getName());
    }
}
