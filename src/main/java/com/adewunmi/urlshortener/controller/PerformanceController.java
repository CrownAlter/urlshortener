package com.adewunmi.urlshortener.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin")
public class PerformanceController {

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/cache/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();

        if (redisTemplate == null) {
            stats.put("status", "Redis not configured - using in-memory cache");
            stats.put("cacheType", "in-memory");
            return ResponseEntity.ok(stats);
        }

        try {
            // Get all keys
            Set<String> urlKeys = redisTemplate.keys("url:mapping:*");
            Set<String> statsKeys = redisTemplate.keys("url:stats:*");
            Set<String> clickKeys = redisTemplate.keys("url:clicks:*");

            stats.put("cachedUrls", urlKeys != null ? urlKeys.size() : 0);
            stats.put("cachedStats", statsKeys != null ? statsKeys.size() : 0);
            stats.put("clickCounters", clickKeys != null ? clickKeys.size() : 0);
            stats.put("totalKeys", redisTemplate.keys("*").size());
            stats.put("status", "healthy");
            stats.put("cacheType", "redis");

        } catch (Exception e) {
            stats.put("status", "error");
            stats.put("error", e.getMessage());
            stats.put("cacheType", "redis-error");
        }

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/cache/clear")
    public ResponseEntity<String> clearCache() {
        if (redisTemplate == null) {
            return ResponseEntity.ok("In-memory cache cannot be manually cleared");
        }
        
        try {
            Set<String> keys = redisTemplate.keys("*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
            return ResponseEntity.ok("Cache cleared successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Failed to clear cache: " + e.getMessage());
        }
    }
}