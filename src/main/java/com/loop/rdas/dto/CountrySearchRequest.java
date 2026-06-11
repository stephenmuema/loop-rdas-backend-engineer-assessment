package com.loop.rdas.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Bound from the query string of {@code GET /api/v1/countries}. Every field is
 * optional; supplying several narrows the result (logical AND).
 *
 * <ul>
 *   <li>{@code name} — partial, case-insensitive match on country name.</li>
 *   <li>{@code continent} — continent code (e.g. {@code AF}) or name (e.g. {@code Africa}).</li>
 *   <li>{@code currency} — currency ISO code (e.g. {@code KES}).</li>
 *   <li>{@code language} — language code (e.g. {@code swa}) or name (e.g. {@code Swahili}).</li>
 * </ul>
 */
@Getter
@Setter
@ToString
public class CountrySearchRequest {

    @Size(max = 100, message = "name filter must be at most 100 characters")
    private String name;

    @Size(max = 64, message = "continent filter must be at most 64 characters")
    private String continent;

    @Size(max = 32, message = "currency filter must be at most 32 characters")
    private String currency;

    @Size(max = 64, message = "language filter must be at most 64 characters")
    private String language;

    public boolean isEmpty() {
        return isBlank(name) && isBlank(continent) && isBlank(currency) && isBlank(language);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
