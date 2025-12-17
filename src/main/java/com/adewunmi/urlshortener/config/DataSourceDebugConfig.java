package com.adewunmi.urlshortener.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DataSourceDebugConfig {

    private final Environment env;

    public DataSourceDebugConfig(Environment env) {
        this.env = env;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logDatabaseConfig() {
        log.info("=== Database Configuration ===");
        log.info("Active Profile: {}", String.join(",", env.getActiveProfiles()));

        String dbUrl = env.getProperty("DATABASE_URL");
        String springDbUrl = env.getProperty("spring.datasource.url");

        log.info("DATABASE_URL env var: {}", dbUrl != null ? maskPassword(dbUrl) : "NOT SET");
        log.info("spring.datasource.url: {}", springDbUrl != null ? maskPassword(springDbUrl) : "NOT SET");
        log.info("==============================");
    }

    private String maskPassword(String url) {
        if (url == null)
            return "null";
        // Mask password in URL for security
        return url.replaceAll("://([^:]+):([^@]+)@", "://$1:****@");
    }
}