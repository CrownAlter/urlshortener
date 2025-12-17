package com.adewunmi.urlshortener.controller;

import com.adewunmi.urlshortener.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Enhanced health check endpoint for Render deployment monitoring.
 * Provides detailed health status of database, cache, and application.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class HealthController {

    private final DataSource dataSource;
    private final CacheService cacheService;

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        boolean isHealthy = true;
        
        health.put("service", "URL Shortener");
        health.put("timestamp", LocalDateTime.now().toString());
        
        // Database health check
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(2)) {
                health.put("database", "UP");
                health.put("databaseType", connection.getMetaData().getDatabaseProductName());
                health.put("databaseUrl", connection.getMetaData().getURL());
            } else {
                health.put("database", "DOWN");
                health.put("databaseError", "Connection invalid");
                isHealthy = false;
            }
        } catch (Exception e) {
            health.put("database", "DOWN");
            health.put("databaseError", e.getMessage());
            isHealthy = false;
            log.error("Database health check failed", e);
        }
        
        // Cache health check
        health.put("cache", cacheService.isRedisEnabled() ? "Redis" : "In-Memory");
        health.put("redisEnabled", cacheService.isRedisEnabled());
        
        // Overall status
        health.put("status", isHealthy ? "UP" : "DOWN");
        
        return ResponseEntity
                .status(isHealthy ? 200 : 503)
                .body(health);
    }

    @GetMapping("/api/health/ready")
    public ResponseEntity<Map<String, Object>> readiness() {
        Map<String, Object> response = new HashMap<>();
        
        // Check if application is ready to serve traffic
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(2)) {
                response.put("status", "READY");
                response.put("database", "connected");
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "NOT READY");
                response.put("reason", "Database connection invalid");
                return ResponseEntity.status(503).body(response);
            }
        } catch (Exception e) {
            response.put("status", "NOT READY");
            response.put("reason", "Database connection failed: " + e.getMessage());
            log.error("Readiness check failed", e);
            return ResponseEntity.status(503).body(response);
        }
    }

    @GetMapping("/api/health/live")
    public ResponseEntity<Map<String, Object>> liveness() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ALIVE");
        response.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }
}
