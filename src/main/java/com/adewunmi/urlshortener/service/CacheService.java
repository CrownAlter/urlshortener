package com.adewunmi.urlshortener.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final boolean redisEnabled;

    // In-memory fallback cache for when Redis is not available
    private final Map<String, String> inMemoryCache = new ConcurrentHashMap<>();
    private final Map<String, Long> clickCounts = new ConcurrentHashMap<>();

    private static final String URL_MAPPING_PREFIX = "url:mapping:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    public CacheService(@Autowired(required = false) RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.redisEnabled = redisTemplate != null;

        if (!redisEnabled) {
            log.warn("Redis is not available. Using in-memory cache (not suitable for production scaling)");
        }
    }

    public void cacheUrlMapping(String shortCode, String originalUrl) {
        if (redisEnabled) {
            try {
                String key = URL_MAPPING_PREFIX + shortCode;
                redisTemplate.opsForValue().set(key, originalUrl, DEFAULT_TTL);
                log.debug("Cached URL mapping in Redis: {} -> {}", shortCode, originalUrl);
            } catch (Exception e) {
                log.error("Failed to cache in Redis, using in-memory fallback", e);
                inMemoryCache.put(shortCode, originalUrl);
            }
        } else {
            inMemoryCache.put(shortCode, originalUrl);
            log.debug("Cached URL mapping in memory: {} -> {}", shortCode, originalUrl);
        }
    }

    public String getCachedUrl(String shortCode) {
        if (redisEnabled) {
            try {
                String key = URL_MAPPING_PREFIX + shortCode;
                Object value = redisTemplate.opsForValue().get(key);

                if (value != null) {
                    log.debug("Cache HIT (Redis) for short code: {}", shortCode);
                    return value.toString();
                }
            } catch (Exception e) {
                log.error("Failed to get from Redis, checking in-memory fallback", e);
                return inMemoryCache.get(shortCode);
            }
        } else {
            String value = inMemoryCache.get(shortCode);
            if (value != null) {
                log.debug("Cache HIT (in-memory) for short code: {}", shortCode);
                return value;
            }
        }

        log.debug("Cache MISS for short code: {}", shortCode);
        return null;
    }

    public void invalidateUrlCache(String shortCode) {
        if (redisEnabled) {
            try {
                String key = URL_MAPPING_PREFIX + shortCode;
                redisTemplate.delete(key);
                log.debug("Invalidated cache in Redis for short code: {}", shortCode);
            } catch (Exception e) {
                log.error("Failed to invalidate Redis cache", e);
            }
        }
        inMemoryCache.remove(shortCode);
    }

    public void incrementClickCount(String shortCode) {
        if (redisEnabled) {
            try {
                String key = "url:clicks:" + shortCode;
                redisTemplate.opsForValue().increment(key);
            } catch (Exception e) {
                log.error("Failed to increment in Redis, using in-memory", e);
                clickCounts.merge(shortCode, 1L, Long::sum);
            }
        } else {
            clickCounts.merge(shortCode, 1L, Long::sum);
        }
    }

    public Long getClickCount(String shortCode) {
        if (redisEnabled) {
            try {
                String key = "url:clicks:" + shortCode;
                Object value = redisTemplate.opsForValue().get(key);
                return value != null ? Long.parseLong(value.toString()) : 0L;
            } catch (Exception e) {
                log.error("Failed to get click count from Redis", e);
            }
        }
        return clickCounts.getOrDefault(shortCode, 0L);
    }

    public boolean isRedisEnabled() {
        return redisEnabled;
    }
}