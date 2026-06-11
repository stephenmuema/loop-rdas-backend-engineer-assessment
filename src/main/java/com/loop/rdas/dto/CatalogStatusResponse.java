package com.loop.rdas.dto;

import java.time.Instant;

/**
 * Operational snapshot of the materialized catalog, returned by the admin
 * status endpoint and surfaced (in summary) by the catalog health indicator.
 */
public record CatalogStatusResponse(
        long countryCount,
        long continentCount,
        long currencyCount,
        long languageCount,
        Instant lastRefreshStartedAt,
        Instant lastRefreshCompletedAt,
        boolean lastRefreshSuccessful,
        String lastError,
        boolean refreshInProgress,
        boolean stale) {
}
