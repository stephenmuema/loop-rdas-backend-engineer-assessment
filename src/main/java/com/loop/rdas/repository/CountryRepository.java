package com.loop.rdas.repository;

import com.loop.rdas.model.Country;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

/**
 * Repository over the materialized country catalog. Extends
 * {@link JpaSpecificationExecutor} to support dynamic filtering with pagination
 * and sorting.
 */
public interface CountryRepository
        extends JpaRepository<Country, Long>, JpaSpecificationExecutor<Country> {

    Optional<Country> findByIsoCodeIgnoreCase(String isoCode);

    List<Country> findByCurrencyCodeIgnoreCaseOrderByNameAsc(String currencyCode);

    boolean existsByIsoCodeIgnoreCase(String isoCode);

    /** Most recent harvest timestamp across the catalog, or {@code null} if empty. */
    @Query("select max(c.sourceRefreshedAt) from Country c")
    java.time.Instant findLatestRefreshedAt();
}
