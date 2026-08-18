package com.jobtracker.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configures Caffeine in-memory cache for storing per-IP rate limit buckets.
 * Each IP address gets its own token bucket stored in this cache.
 * Entries expire after 2 minutes of inactivity to prevent memory leaks.
 */
@Configuration
@EnableCaching
public class RateLimitConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("rate-limit-buckets");
        manager.setCaffeine(
            Caffeine.newBuilder()
                .expireAfterAccess(2, TimeUnit.MINUTES)
                .maximumSize(10_000)
        );
        return manager;
    }
}
