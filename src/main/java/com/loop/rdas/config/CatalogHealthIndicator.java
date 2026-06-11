package com.loop.rdas.config;

import com.loop.rdas.dto.CatalogStatusResponse;
import com.loop.rdas.service.CountryQueryService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Surfaces the materialized catalog's state on {@code /actuator/health}.
 *
 * <p>The catalog is the serving substrate, so an empty catalog is reported as
 * {@code DOWN} (it needs operator attention), a stale catalog as
 * {@code UP} with a {@code stale=true} marker (degraded but still serving), and
 * a populated, fresh catalog as {@code UP}. This indicator is intentionally NOT
 * part of the Kubernetes readiness group, so a SOAP outage that prevents a
 * refresh never ejects an otherwise-serving pod.</p>
 */
@Component("catalog")
public class CatalogHealthIndicator implements HealthIndicator {

    private final CountryQueryService queryService;

    public CatalogHealthIndicator(CountryQueryService queryService) {
        this.queryService = queryService;
    }

    @Override
    public Health health() {
        CatalogStatusResponse s = queryService.getCatalogStatus();
        Health.Builder builder = s.countryCount() > 0 ? Health.up() : Health.down();
        return builder
                .withDetail("countryCount", s.countryCount())
                .withDetail("continentCount", s.continentCount())
                .withDetail("currencyCount", s.currencyCount())
                .withDetail("languageCount", s.languageCount())
                .withDetail("lastRefreshCompletedAt", String.valueOf(s.lastRefreshCompletedAt()))
                .withDetail("lastRefreshSuccessful", s.lastRefreshSuccessful())
                .withDetail("refreshInProgress", s.refreshInProgress())
                .withDetail("stale", s.stale())
                .build();
    }
}
