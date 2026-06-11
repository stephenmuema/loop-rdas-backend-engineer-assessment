package com.loop.rdas.config;

import com.loop.rdas.repository.CountryRepository;
import com.loop.rdas.service.CatalogHarvestService;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * On startup, triggers a background catalog harvest if the catalog is empty or
 * stale. The harvest is asynchronous, so the application becomes ready and
 * serves immediately from whatever data the persistent catalog already holds —
 * even if the SOAP service is currently unavailable.
 */
@Component
public class StartupHarvestRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupHarvestRunner.class);

    private final CountryRepository countryRepository;
    private final CatalogHarvestService harvestService;

    @Value("${rdas.harvest.run-on-startup:true}")
    private boolean runOnStartup;

    @Value("${rdas.harvest.stale-after-hours:24}")
    private long staleAfterHours;

    public StartupHarvestRunner(CountryRepository countryRepository,
                                CatalogHarvestService harvestService) {
        this.countryRepository = countryRepository;
        this.harvestService = harvestService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!runOnStartup) {
            log.info("Startup harvest disabled (rdas.harvest.run-on-startup=false)");
            return;
        }
        Instant latest = countryRepository.findLatestRefreshedAt();
        boolean empty = countryRepository.count() == 0;
        boolean stale = latest == null
                || Duration.between(latest, Instant.now()).toHours() >= staleAfterHours;

        if (empty || stale) {
            log.info("Catalog is {} on startup; triggering background harvest",
                    empty ? "empty" : "stale");
            harvestService.refreshAsync();
        } else {
            log.info("Catalog is fresh on startup (last refresh {}); skipping harvest", latest);
        }
    }
}
