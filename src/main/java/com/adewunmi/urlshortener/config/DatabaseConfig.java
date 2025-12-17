package com.adewunmi.urlshortener.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Database configuration for production deployment on Render.
 * Converts Render's DATABASE_URL format (postgres://user:password@host:port/database)
 * to JDBC format (jdbc:postgresql://host:port/database)
 */
@Configuration
@Profile("prod")
@Slf4j
public class DatabaseConfig {

    @Bean
    public DataSource dataSource() {
        String databaseUrl = System.getenv("DATABASE_URL");
        
        if (databaseUrl != null && databaseUrl.startsWith("postgres://")) {
            log.info("Converting Render DATABASE_URL to JDBC format");
            
            try {
                URI dbUri = new URI(databaseUrl);
                
                String username = dbUri.getUserInfo().split(":")[0];
                String password = dbUri.getUserInfo().split(":")[1];
                String jdbcUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath();
                
                log.info("Database connection configured for host: {}", dbUri.getHost());
                
                return DataSourceBuilder
                        .create()
                        .url(jdbcUrl)
                        .username(username)
                        .password(password)
                        .driverClassName("org.postgresql.Driver")
                        .build();
                        
            } catch (URISyntaxException e) {
                log.error("Failed to parse DATABASE_URL", e);
                throw new RuntimeException("Invalid DATABASE_URL format", e);
            }
        }
        
        // Fallback to default configuration from application-prod.properties
        log.info("Using default database configuration from properties");
        return DataSourceBuilder.create().build();
    }
}
