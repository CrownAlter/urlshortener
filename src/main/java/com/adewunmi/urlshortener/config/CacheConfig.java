package com.adewunmi.urlshortener.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * Cache configuration with fallback to in-memory caching when Redis is not available.
 * This is essential for Render's free tier where Redis is not provided.
 */
@Configuration
@EnableCaching
@Slf4j
public class CacheConfig {

    /**
     * Fallback cache manager using in-memory ConcurrentHashMap.
     * This bean is only created if no other CacheManager bean is present (i.e., Redis is not configured).
     */
    @Bean
    @ConditionalOnMissingBean(RedisConnectionFactory.class)
    public CacheManager cacheManager() {
        log.warn("Redis not available - using in-memory cache (not distributed, suitable for single instance only)");
        return new ConcurrentMapCacheManager("urlMappings", "urlStats");
    }
}
