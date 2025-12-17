package com.adewunmi.urlshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClickByDateDto {
    private String date; // Format: "2024-01-15"
    private Long clicks;
}