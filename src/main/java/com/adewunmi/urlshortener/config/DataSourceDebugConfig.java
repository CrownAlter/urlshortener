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
public class DataSourceDebugConfig {

    @Bean
    @Primary
    public DataSource dataSource() {
        String databaseUrl = System.getenv("DATABASE_URL");

        log.info("=== DataSource Configuration ===");
        log.info("DATABASE_URL present: {}", databaseUrl != null && !databaseUrl.isEmpty());

        if (databaseUrl == null || databaseUrl.isEmpty()) {
            throw new IllegalStateException(
                    "DATABASE_URL environment variable is not set. " +
                            "Please configure it in Render dashboard.");
        }

        try {
            log.info("Parsing DATABASE_URL...");
            URI dbUri = new URI(databaseUrl);

            String[] userInfo = dbUri.getUserInfo().split(":");
            String username = userInfo[0];
            String password = userInfo[1];
            String host = dbUri.getHost();
            int port = dbUri.getPort();
            String database = dbUri.getPath().substring(1);

            String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s", host, port, database);

            log.info("JDBC URL: {}", jdbcUrl);
            log.info("Database: {}", database);
            log.info("Host: {}", host);
            log.info("Port: {}", port);
            log.info("Username: {}", username);
            log.info("================================");

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName("org.postgresql.Driver");

            // Connection pool settings optimized for Render free tier
            config.setMaximumPoolSize(5);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            config.setLeakDetectionThreshold(60000);

            // SSL settings for Render PostgreSQL
            config.addDataSourceProperty("ssl", "true");
            config.addDataSourceProperty("sslmode", "require");

            log.info("HikariCP DataSource configured successfully");

            return new HikariDataSource(config);

        } catch (URISyntaxException e) {
            log.error("Failed to parse DATABASE_URL: {}", e.getMessage());
            log.error("DATABASE_URL format should be: postgresql://user:password@host:port/database");
            throw new IllegalStateException("Invalid DATABASE_URL format", e);
        } catch (Exception e) {
            log.error("Unexpected error configuring DataSource: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to configure DataSource", e);
        }
    }
}