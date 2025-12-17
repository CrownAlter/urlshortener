package com.adewunmi.urlshortener.service;

import com.adewunmi.urlshortener.dto.ShortenUrlRequest;
import com.adewunmi.urlshortener.dto.ShortenUrlResponse;
import com.adewunmi.urlshortener.entity.Click;
import com.adewunmi.urlshortener.entity.Url;
import com.adewunmi.urlshortener.repository.ClickRepository;
import com.adewunmi.urlshortener.repository.UrlRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlService {

    private final UrlRepository urlRepository;
    private final ClickRepository clickRepository;
    private final CacheService cacheService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int SHORT_CODE_LENGTH = 7;
    private static final int MAX_CUSTOM_CODE_LENGTH = 20;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public ShortenUrlResponse shortenUrl(ShortenUrlRequest request) {
        // Validate URL format
        validateUrl(request.getUrl());

        // Check if URL was already shortened
        Optional<Url> existingUrl = urlRepository.findByOriginalUrl(request.getUrl());
        if (existingUrl.isPresent()) {
            Url url = existingUrl.get();
            String shortUrl = baseUrl + "/" + url.getShortCode();

            // Ensure it's cached
            cacheService.cacheUrlMapping(url.getShortCode(), url.getOriginalUrl());

            log.info("Returning existing short URL for: {}", request.getUrl());
            return new ShortenUrlResponse(url.getOriginalUrl(), shortUrl, url.getShortCode());
        }

        String shortCode;

        // If custom code provided, validate and use it
        if (request.getCustomCode() != null && !request.getCustomCode().isEmpty()) {
            shortCode = validateAndSanitizeCustomCode(request.getCustomCode());
            if (urlRepository.existsByShortCode(shortCode)) {
                throw new IllegalArgumentException("Custom short code '" + shortCode + "' is already in use");
            }
        } else {
            // Generate random short code
            shortCode = generateShortCode();
        }

        Url url = new Url();
        url.setOriginalUrl(request.getUrl());
        url.setShortCode(shortCode);

        url = urlRepository.save(url);

        // Cache the mapping
        cacheService.cacheUrlMapping(shortCode, request.getUrl());

        String shortUrl = baseUrl + "/" + shortCode;

        log.info("Created short URL: {} -> {}", shortCode, request.getUrl());

        return new ShortenUrlResponse(url.getOriginalUrl(), shortUrl, shortCode);
    }

    @Transactional
    public String getOriginalUrl(String shortCode, HttpServletRequest request) {
        // Try to get from cache first
        String cachedUrl = cacheService.getCachedUrl(shortCode);

        if (cachedUrl != null) {
            log.debug("Retrieved URL from cache: {}", shortCode);
            // Still track the click
            trackClickAsync(shortCode, request);
            return cachedUrl;
        }

        // Cache miss - get from database
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new IllegalArgumentException("Short code '" + shortCode + "' not found"));

        // Check if URL is expired
        if (url.getExpiresAt() != null && url.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Attempted to access expired URL: {}", shortCode);
            throw new IllegalArgumentException("This short URL has expired");
        }

        // Cache it for next time
        cacheService.cacheUrlMapping(shortCode, url.getOriginalUrl());

        // Track the click
        trackClick(url, request);

        log.info("Redirecting {} to {} (Total clicks: {})",
                shortCode, url.getOriginalUrl(), clickRepository.countByUrlId(url.getId()));

        return url.getOriginalUrl();
    }

    private void trackClick(Url url, HttpServletRequest request) {
        Click click = new Click();
        click.setUrl(url);
        click.setIpAddress(getClientIp(request));
        click.setUserAgent(request.getHeader("User-Agent"));
        click.setReferrer(request.getHeader("Referer"));

        clickRepository.save(click);

        // Increment Redis counter for quick stats
        cacheService.incrementClickCount(url.getShortCode());
    }

    private void trackClickAsync(String shortCode, HttpServletRequest request) {
        // For cached URLs, we still need to track clicks
        // This is a simplified version - in production, use @Async
        Url url = urlRepository.findByShortCode(shortCode).orElse(null);
        if (url != null) {
            trackClick(url, request);
        }
    }

    private void validateUrl(String urlString) {
        try {
            URL url = new URL(urlString);

            // Check protocol
            if (!url.getProtocol().equals("http") && !url.getProtocol().equals("https")) {
                throw new IllegalArgumentException("URL must use HTTP or HTTPS protocol");
            }

            // Check if host is valid
            if (url.getHost() == null || url.getHost().isEmpty()) {
                throw new IllegalArgumentException("URL must have a valid host");
            }

        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL format: " + e.getMessage());
        }
    }

    private String validateAndSanitizeCustomCode(String customCode) {
        customCode = customCode.trim();

        if (customCode.length() < 3) {
            throw new IllegalArgumentException("Custom code must be at least 3 characters long");
        }

        if (customCode.length() > MAX_CUSTOM_CODE_LENGTH) {
            throw new IllegalArgumentException("Custom code must not exceed " + MAX_CUSTOM_CODE_LENGTH + " characters");
        }

        if (!customCode.matches("^[a-zA-Z0-9_-]+$")) {
            throw new IllegalArgumentException(
                    "Custom code can only contain letters, numbers, hyphens, and underscores");
        }

        String[] reservedWords = { "api", "admin", "stats", "health", "docs", "swagger" };
        for (String reserved : reservedWords) {
            if (customCode.equalsIgnoreCase(reserved)) {
                throw new IllegalArgumentException("Custom code '" + customCode + "' is reserved");
            }
        }

        return customCode;
    }

    private String generateShortCode() {
        String shortCode;
        int attempts = 0;
        int maxAttempts = 10;

        do {
            shortCode = generateRandomString();
            attempts++;

            if (attempts >= maxAttempts) {
                log.error("Failed to generate unique short code after {} attempts", maxAttempts);
                throw new RuntimeException("Unable to generate unique short code. Please try again.");
            }
        } while (urlRepository.existsByShortCode(shortCode));

        return shortCode;
    }

    private String generateRandomString() {
        StringBuilder sb = new StringBuilder(SHORT_CODE_LENGTH);
        for (int i = 0; i < SHORT_CODE_LENGTH; i++) {
            sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
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