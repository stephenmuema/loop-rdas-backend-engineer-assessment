package com.loop.rdas.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * In-process Caffeine cache for the hot read paths (country search results,
 * country detail, currency peers and the reference lists).
 *
 * <p>The materialized catalog in the database is the primary cache of SOAP
 * data; this layer sits in front of the database to absorb repeated identical
 * queries. The authoritative invalidation is an evict-all after every
 * successful harvest (see {@code CatalogHarvestService}); the TTL here is only
 * a safety net so nothing is served indefinitely between refreshes.</p>
 */
@Configuration
public class CacheConfig {

    /** Cache names used across the service. */
    public static final String COUNTRY_SEARCH = "countrySearch";
    public static final String COUNTRY_BY_ISO = "countryByIso";
    public static final String CURRENCY_PEERS = "currencyPeers";
    public static final String REFERENCE_LIST = "referenceList";

    @Value("${cache.ttl-minutes:30}")
    private long ttlMinutes;

    @Value("${cache.max-size:10000}")
    private long maxSize;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(
                COUNTRY_SEARCH, COUNTRY_BY_ISO, CURRENCY_PEERS, REFERENCE_LIST);
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(ttlMinutes))
                .maximumSize(maxSize)
                .recordStats());
        return manager;
    }
}
