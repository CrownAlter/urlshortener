package com.adewunmi.urlshortener.controller;

import com.adewunmi.urlshortener.dto.ShortenUrlRequest;
import com.adewunmi.urlshortener.dto.ShortenUrlResponse;
import com.adewunmi.urlshortener.dto.UrlListResponse;
import com.adewunmi.urlshortener.dto.UrlStatsResponse;
import com.adewunmi.urlshortener.service.AnalyticsService;
import com.adewunmi.urlshortener.service.RateLimitService;
import com.adewunmi.urlshortener.service.UrlService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class UrlController {

    private final UrlService urlService;
    private final AnalyticsService analyticsService;
    private final RateLimitService rateLimitService;

    @PostMapping("/api/shorten")
    public ResponseEntity<?> shortenUrl(
            @Valid @RequestBody ShortenUrlRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = getClientIp(httpRequest);

        // Check rate limit
        if (!rateLimitService.allowShortenRequest(clientIp)) {
            log.warn("Rate limit exceeded for IP: {}", clientIp);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Rate limit exceeded. Please try again later.");
        }

        try {
            ShortenUrlResponse response = urlService.shortenUrl(request);

            // Add rate limit headers
            long remaining = rateLimitService.getRemainingTokens(clientIp, "shorten");
            return ResponseEntity.status(HttpStatus.CREATED)
                    .header("X-RateLimit-Remaining", String.valueOf(remaining))
                    .body(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request to shorten URL: {}", e.getMessage());
            throw e;
        }
    }

    @GetMapping("/{shortCode}")
    public RedirectView redirect(@PathVariable String shortCode,
            HttpServletRequest request) {

        String clientIp = getClientIp(request);

        // Check rate limit
        if (!rateLimitService.allowRedirectRequest(clientIp)) {
            log.warn("Rate limit exceeded for redirects from IP: {}", clientIp);
            throw new IllegalArgumentException("Too many requests. Please slow down.");
        }

        try {
            String originalUrl = urlService.getOriginalUrl(shortCode, request);
            RedirectView redirectView = new RedirectView(originalUrl);
            redirectView.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
            return redirectView;
        } catch (IllegalArgumentException e) {
            log.warn("Short code not found or error: {}", e.getMessage());
            throw e;
        }
    }

    @GetMapping("/api/stats/{shortCode}")
    public ResponseEntity<UrlStatsResponse> getStats(@PathVariable String shortCode) {
        try {
            UrlStatsResponse stats = analyticsService.getUrlStats(shortCode);
            return ResponseEntity.ok(stats);
        } catch (IllegalArgumentException e) {
            log.warn("Stats requested for non-existent short code: {}", shortCode);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/api/urls")
    public ResponseEntity<List<UrlListResponse>> getAllUrls() {
        List<UrlListResponse> urls = analyticsService.getAllUrls();
        return ResponseEntity.ok(urls);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }
}