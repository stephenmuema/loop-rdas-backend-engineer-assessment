package com.loop.rdas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Reference Data Aggregation Service (RDAS).
 *
 * <p>RDAS is the single source of truth for country, currency, language and
 * geographical reference data at LOOP DFS. It exposes clean REST/JSON APIs to
 * every channel while consuming the third-party CountryInfo SOAP service
 * internally.</p>
 *
 * <p>The service maintains a <em>materialized catalog</em> of all countries in
 * its own datastore. A scheduled, rate-limited harvester is the only component
 * that talks to SOAP; every read API (search, filter, sort, paginate, details,
 * currency peers) is served entirely from the local catalog and an in-process
 * cache, so the request path makes zero SOAP calls.</p>
 */
@SpringBootApplication
@EnableScheduling
@EnableCaching
public class RdasApplication {

    public static void main(String[] args) {
        SpringApplication.run(RdasApplication.class, args);
    }
}
