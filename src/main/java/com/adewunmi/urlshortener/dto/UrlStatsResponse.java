package com.adewunmi.urlshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlStatsResponse {
    private String shortCode;
    private String originalUrl;
    private LocalDateTime createdAt;
    private Long totalClicks;
    private List<ClickByDateDto> clicksByDate;
    private List<TopReferrerDto> topReferrers;
    private List<DeviceStatsDto> deviceStats;
    private List<RecentClickDto> recentClicks;
}