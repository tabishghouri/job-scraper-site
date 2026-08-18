package com.jobtracker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS configuration — allows the React frontend to call this API.
 * Allowed origins come from app.cors.allowed-origins (CORS_ALLOWED_ORIGINS env var),
 * so deploying to a new frontend URL is a config change, not a code change.
 * Also exposes the scraper API key so the interceptor can read it.
 */
@Configuration
public class SecurityConfig implements WebMvcConfigurer {

    @Value("${app.scraper.api-key}")
    public String scraperApiKey;

    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("X-RateLimit-Remaining", "X-RateLimit-Limit");
    }
}
