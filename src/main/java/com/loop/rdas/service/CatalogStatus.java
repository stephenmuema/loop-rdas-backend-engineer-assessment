package com.loop.rdas.service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;

/**
 * Thread-safe holder for the live state of the catalog harvester. Updated by
 * {@link CatalogHarvestService} and read by the admin status endpoint and the
 * catalog health indicator.
 */
@Component
public class CatalogStatus {

    private final AtomicBoolean refreshInProgress = new AtomicBoolean(false);
    private volatile Instant lastStartedAt;
    private volatile Instant lastCompletedAt;
    private volatile boolean lastSuccessful;
    private volatile String lastError;

    /** Attempts to claim the single-flight refresh slot. */
    public boolean beginRefresh() {
        boolean acquired = refreshInProgress.compareAndSet(false, true);
        if (acquired) {
            lastStartedAt = Instant.now();
        }
        return acquired;
    }

    public void completeSuccess() {
        lastCompletedAt = Instant.now();
        lastSuccessful = true;
        lastError = null;
        refreshInProgress.set(false);
    }

    public void completeFailure(String error) {
        lastCompletedAt = Instant.now();
        lastSuccessful = false;
        lastError = error;
        refreshInProgress.set(false);
    }

    public boolean isRefreshInProgress() {
        return refreshInProgress.get();
    }

    public Instant getLastStartedAt() {
        return lastStartedAt;
    }

    public Instant getLastCompletedAt() {
        return lastCompletedAt;
    }

    public boolean isLastSuccessful() {
        return lastSuccessful;
    }

    public String getLastError() {
        return lastError;
    }
}
