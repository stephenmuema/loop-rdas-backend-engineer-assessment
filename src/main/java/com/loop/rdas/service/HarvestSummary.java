package com.loop.rdas.service;

/**
 * Outcome of a catalog harvest run.
 */
public record HarvestSummary(
        boolean started,
        boolean successful,
        int countriesProcessed,
        int countriesFailed,
        int continents,
        int currencies,
        int languages,
        long durationMs,
        String message) {

    public static HarvestSummary alreadyRunning() {
        return new HarvestSummary(false, false, 0, 0, 0, 0, 0, 0,
                "A harvest is already in progress");
    }
}
