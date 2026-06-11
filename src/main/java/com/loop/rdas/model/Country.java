package com.loop.rdas.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

/**
 * A country entry in the materialized reference-data catalog.
 *
 * <p>The ISO code is the natural, public, non-sensitive identifier for a
 * country and is what every API exposes. The numeric {@code id} is an internal
 * surrogate key only. (Unlike user-owned records, public reference data carries
 * no IDOR risk, so a guessable identifier is appropriate here.)</p>
 */
@Entity
@Table(
        name = "country",
        indexes = {
                @Index(name = "ux_country_iso", columnList = "iso_code", unique = true),
                @Index(name = "ix_country_name", columnList = "name"),
                @Index(name = "ix_country_continent", columnList = "continent_code"),
                @Index(name = "ix_country_currency", columnList = "currency_code")
        })
@Getter
@Setter
public class Country {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "iso_code", nullable = false, unique = true, length = 8)
    private String isoCode;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "capital_city", length = 128)
    private String capitalCity;

    @Column(name = "phone_code", length = 16)
    private String phoneCode;

    @Column(name = "continent_code", length = 8)
    private String continentCode;

    @Column(name = "continent_name", length = 64)
    private String continentName;

    @Column(name = "currency_code", length = 8)
    private String currencyCode;

    @Column(name = "currency_name", length = 128)
    private String currencyName;

    @Column(name = "flag_url", length = 1024)
    private String flagUrl;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "country_language",
            joinColumns = @JoinColumn(name = "country_id"))
    private Set<CountryLanguage> languages = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** When this entry was last (re)harvested from the SOAP source. */
    @Column(name = "source_refreshed_at")
    private Instant sourceRefreshedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void replaceLanguages(Set<CountryLanguage> newLanguages) {
        this.languages.clear();
        if (newLanguages != null) {
            this.languages.addAll(newLanguages);
        }
    }
}
