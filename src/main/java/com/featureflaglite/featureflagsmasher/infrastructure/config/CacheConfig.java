package com.featureflaglite.featureflagsmasher.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache configuration using Caffeine for in-memory caching.
 * Suitable for a single-instance application.
 */
@Configuration
public class CacheConfig {

    public static final String CACHE_ENVIRONMENT_FLAGS = "environmentFlags";
    public static final String CACHE_FLAG_STATES = "flagStates";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                CACHE_ENVIRONMENT_FLAGS,
                CACHE_FLAG_STATES
        );
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats());
        return cacheManager;
    }
}
