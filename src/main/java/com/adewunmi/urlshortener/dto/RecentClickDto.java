package com.adewunmi.urlshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecentClickDto {
    private LocalDateTime clickedAt;
    private String ipAddress;
    private String referrer;
    private String deviceType;
}