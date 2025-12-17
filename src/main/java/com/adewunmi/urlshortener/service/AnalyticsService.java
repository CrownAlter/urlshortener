package com.adewunmi.urlshortener.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.adewunmi.urlshortener.dto.ClickByDateDto;
import com.adewunmi.urlshortener.dto.DeviceStatsDto;
import com.adewunmi.urlshortener.dto.RecentClickDto;
import com.adewunmi.urlshortener.dto.TopReferrerDto;
import com.adewunmi.urlshortener.dto.UrlListResponse;
import com.adewunmi.urlshortener.dto.UrlStatsResponse;
import com.adewunmi.urlshortener.entity.Click;
import com.adewunmi.urlshortener.entity.Url;
import com.adewunmi.urlshortener.repository.ClickRepository;
import com.adewunmi.urlshortener.repository.UrlRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final UrlRepository urlRepository;
    private final ClickRepository clickRepository;

    @Transactional(readOnly = true)
    public UrlStatsResponse getUrlStats(String shortCode) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new IllegalArgumentException("Short code not found"));

        Long totalClicks = clickRepository.countByUrlId(url.getId());

        // Get clicks by date
        List<ClickByDateDto> clicksByDate = getClicksByDate(url.getId());

        // Get top referrers
        List<TopReferrerDto> topReferrers = getTopReferrers(url.getId());

        // Get device stats
        List<DeviceStatsDto> deviceStats = getDeviceStats(url.getId());

        // Get recent clicks
        List<RecentClickDto> recentClicks = getRecentClicks(url.getId());

        return UrlStatsResponse.builder()
                .shortCode(url.getShortCode())
                .originalUrl(url.getOriginalUrl())
                .createdAt(url.getCreatedAt())
                .totalClicks(totalClicks)
                .clicksByDate(clicksByDate)
                .topReferrers(topReferrers)
                .deviceStats(deviceStats)
                .recentClicks(recentClicks)
                .build();
    }

    private List<ClickByDateDto> getClicksByDate(Long urlId) {
        List<Object[]> results = clickRepository.findClicksByDate(urlId);

        return results.stream()
                .map(row -> new ClickByDateDto(
                        row[0].toString(),
                        ((Number) row[1]).longValue()))
                .collect(Collectors.toList());
    }

    private List<TopReferrerDto> getTopReferrers(Long urlId) {
        List<Object[]> results = clickRepository.findTopReferrers(urlId);

        return results.stream()
                .limit(5) // Top 5 referrers
                .map(row -> new TopReferrerDto(
                        row[0] != null ? row[0].toString() : "Direct",
                        ((Number) row[1]).longValue()))
                .collect(Collectors.toList());
    }

    private List<DeviceStatsDto> getDeviceStats(Long urlId) {
        // Get all clicks for this URL
        List<Click> clicks = clickRepository.findByUrlIdAndClickedAtAfter(
                urlId,
                LocalDateTime.now().minusDays(30));

        // Group by device type
        Map<String, Long> deviceCounts = new HashMap<>();

        for (Click click : clicks) {
            String deviceType = determineDeviceType(click.getUserAgent());
            deviceCounts.put(deviceType, deviceCounts.getOrDefault(deviceType, 0L) + 1);
        }

        return deviceCounts.entrySet().stream()
                .map(entry -> new DeviceStatsDto(entry.getKey(), entry.getValue()))
                .sorted((a, b) -> Long.compare(b.getClicks(), a.getClicks()))
                .collect(Collectors.toList());
    }

    private List<RecentClickDto> getRecentClicks(Long urlId) {
        List<Click> recentClicks = clickRepository.findTop10ByUrlIdOrderByClickedAtDesc(urlId);

        return recentClicks.stream()
                .map(click -> new RecentClickDto(
                        click.getClickedAt(),
                        maskIpAddress(click.getIpAddress()),
                        click.getReferrer() != null ? click.getReferrer() : "Direct",
                        determineDeviceType(click.getUserAgent())))
                .collect(Collectors.toList());
    }

    private String determineDeviceType(String userAgent) {
        if (userAgent == null) {
            return "Unknown";
        }

        userAgent = userAgent.toLowerCase();

        if (userAgent.contains("mobile") || userAgent.contains("android") ||
                userAgent.contains("iphone")) {
            return "Mobile";
        } else if (userAgent.contains("tablet") || userAgent.contains("ipad")) {
            return "Tablet";
        } else if (userAgent.contains("windows") || userAgent.contains("macintosh") ||
                userAgent.contains("linux")) {
            return "Desktop";
        }

        return "Unknown";
    }

    private String maskIpAddress(String ip) {
        if (ip == null) {
            return "Unknown";
        }

        // Mask last octet for privacy: 192.168.1.1 -> 192.168.1.xxx
        String[] parts = ip.split("\\.");
        if (parts.length == 4) {
            return parts[0] + "." + parts[1] + "." + parts[2] + ".xxx";
        }

        return "xxx.xxx.xxx.xxx";
    }

    @Transactional(readOnly = true)
    public List<UrlListResponse> getAllUrls() {
        List<Url> urls = urlRepository.findAllWithClicks();

        return urls.stream()
                .map(url -> {
                    Long clickCount = clickRepository.countByUrlId(url.getId());
                    return new UrlListResponse(
                            url.getId(),
                            url.getOriginalUrl(),
                            url.getShortCode(),
                            "http://localhost:8080/" + url.getShortCode(), // Will be dynamic later
                            url.getCreatedAt(),
                            clickCount);
                })
                .collect(Collectors.toList());
    }
}
