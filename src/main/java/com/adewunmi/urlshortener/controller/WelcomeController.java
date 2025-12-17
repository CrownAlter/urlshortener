package com.adewunmi.urlshortener.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class WelcomeController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> welcome() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "URL Shortener API");
        response.put("status", "running");
        response.put("version", "1.0.0");
        
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("health", "/api/health");
        endpoints.put("shorten", "POST /api/shorten");
        endpoints.put("redirect", "GET /{shortCode}");
        endpoints.put("stats", "GET /api/stats/{shortCode}");
        endpoints.put("urls", "GET /api/urls");
        
        response.put("endpoints", endpoints);
        
        return ResponseEntity.ok(response);
    }
}
