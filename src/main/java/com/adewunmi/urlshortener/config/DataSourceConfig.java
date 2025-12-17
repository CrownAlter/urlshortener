package com.adewunmi.urlshortener.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;

@Configuration
@Profile("prod")
@Slf4j
public class DataSourceConfig {

    @Bean
    @Primary
    public DataSource dataSource() {
        String databaseUrl = System.getenv("DATABASE_URL");

        log.info("=== Configuring DataSource ===");
        log.info("DATABASE_URL is set: {}", databaseUrl != null && !databaseUrl.isEmpty());

        if (databaseUrl == null || databaseUrl.isEmpty()) {
            log.error("DATABASE_URL environment variable is not set!");
            throw new IllegalStateException(
                    "DATABASE_URL environment variable is required. " +
                            "Please set it in Render dashboard: Environment tab");
        }

        try {
            URI dbUri = new URI(databaseUrl);

            String[] userInfo = dbUri.getUserInfo().split(":");
            String username = userInfo[0];
            String password = userInfo[1];
            String host = dbUri.getHost();
            int port = dbUri.getPort();
            String database = dbUri.getPath().substring(1);

            String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s", host, port, database);

            log.info("Database Host: {}", host);
            log.info("Database Port: {}", port);
            log.info("Database Name: {}", database);
            log.info("Username: {}", username);
            log.info("JDBC URL: {}", jdbcUrl);

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName("org.postgresql.Driver");

            // Optimized for Render free tier
            config.setMaximumPoolSize(5);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);

            // SSL for Render PostgreSQL
            config.addDataSourceProperty("ssl", "true");
            config.addDataSourceProperty("sslmode", "require");

            log.info("DataSource configured successfully!");
            log.info("==============================");

            return new HikariDataSource(config);

        } catch (URISyntaxException e) {
            log.error("Failed to parse DATABASE_URL: {}", e.getMessage());
            throw new IllegalStateException(
                    "Invalid DATABASE_URL format. Expected: postgresql://user:pass@host:port/db", e);
        }
    }
}