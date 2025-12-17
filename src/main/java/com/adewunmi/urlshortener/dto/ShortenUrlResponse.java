package com.adewunmi.urlshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ShortenUrlResponse {
    private String originalUrl;
    private String shortUrl;
    private String shortCode;
}
