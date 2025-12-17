package com.adewunmi.urlshortener.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class RateLimitService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final boolean redisEnabled;

    public RateLimitService(@Autowired(required = false) RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.redisEnabled = redisTemplate != null;
        
        if (!redisEnabled) {
            log.warn("Redis is not available. Rate limiting will use in-memory buckets only.");
        }
    }

    @Value("${rate.limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${rate.limit.shorten.capacity:10}")
    private long shortenCapacity;

    @Value("${rate.limit.shorten.refill-tokens:10}")
    private long shortenRefillTokens;

    @Value("${rate.limit.shorten.refill-duration-minutes:60}")
    private long shortenRefillDuration;

    @Value("${rate.limit.redirect.capacity:100}")
    private long redirectCapacity;

    @Value("${rate.limit.redirect.refill-tokens:100}")
    private long redirectRefillTokens;

    @Value("${rate.limit.redirect.refill-duration-minutes:1}")
    private long redirectRefillDuration;

    // In-memory cache for buckets (for development)
    // In production, consider using Redis-backed buckets
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public boolean allowShortenRequest(String clientIp) {
        if (!rateLimitEnabled) {
            return true;
        }

        String key = "rate:shorten:" + clientIp;
        Bucket bucket = getBucket(key, shortenCapacity, shortenRefillTokens,
                Duration.ofMinutes(shortenRefillDuration));

        boolean allowed = bucket.tryConsume(1);

        if (!allowed) {
            log.warn("Rate limit exceeded for shortening from IP: {}", clientIp);
        }

        return allowed;
    }

    public boolean allowRedirectRequest(String clientIp) {
        if (!rateLimitEnabled) {
            return true;
        }

        String key = "rate:redirect:" + clientIp;
        Bucket bucket = getBucket(key, redirectCapacity, redirectRefillTokens,
                Duration.ofMinutes(redirectRefillDuration));

        boolean allowed = bucket.tryConsume(1);

        if (!allowed) {
            log.warn("Rate limit exceeded for redirects from IP: {}", clientIp);
        }

        return allowed;
    }

    public long getRemainingTokens(String clientIp, String operation) {
        if (!rateLimitEnabled) {
            return Long.MAX_VALUE;
        }

        String key = "rate:" + operation + ":" + clientIp;
        Bucket bucket = buckets.get(key);

        if (bucket == null) {
            return operation.equals("shorten") ? shortenCapacity : redirectCapacity;
        }

        return bucket.getAvailableTokens();
    }

    private Bucket getBucket(String key, long capacity, long refillTokens, Duration refillDuration) {
        return buckets.computeIfAbsent(key, k -> {
            Bandwidth limit = Bandwidth.classic(capacity,
                    Refill.intervally(refillTokens, refillDuration));
            return Bucket.builder()
                    .addLimit(limit)
                    .build();
        });
    }

    // Redis-based rate limiting (alternative implementation)
    public boolean checkRateLimitRedis(String key, int maxRequests, Duration window) {
        if (!redisEnabled) {
            log.warn("Redis not available for rate limiting, using in-memory buckets");
            return true; // Fallback to allowing the request
        }
        
        try {
            String redisKey = "ratelimit:" + key;
            Long currentCount = redisTemplate.opsForValue().increment(redisKey);

            if (currentCount == null) {
                currentCount = 0L;
            }

            if (currentCount == 1) {
                redisTemplate.expire(redisKey, window);
            }

            return currentCount <= maxRequests;
        } catch (Exception e) {
            log.error("Failed to check rate limit in Redis", e);
            return true; // Fail open - allow the request
        }
    }
}