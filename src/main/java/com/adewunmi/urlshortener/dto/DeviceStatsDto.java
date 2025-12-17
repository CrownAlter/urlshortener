package com.adewunmi.urlshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceStatsDto {
    private String deviceType; // "Mobile", "Desktop", "Tablet", "Unknown"
    private Long clicks;
}