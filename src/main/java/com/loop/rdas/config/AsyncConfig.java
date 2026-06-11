package com.loop.rdas.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Enables {@code @Async} and provides a small, single-threaded executor for the
 * catalog harvester. A single worker keeps SOAP calls serialized, which (with
 * the rate limiter and pacing) guarantees the provider quota is respected.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "harvestExecutor")
    public Executor harvestExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(5);
        executor.setThreadNamePrefix("rdas-harvest-");
        executor.initialize();
        return executor;
    }
}
