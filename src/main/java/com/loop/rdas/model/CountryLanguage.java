package com.loop.rdas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A language spoken in a country, stored as an embedded element of the
 * {@link Country} aggregate (table {@code country_language}).
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CountryLanguage {

    @Column(name = "language_code", length = 16)
    private String code;

    @Column(name = "language_name", length = 128)
    private String name;
}
