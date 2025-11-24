package com.example.userservice.config;

import feign.Logger;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for OpenFeign.
 * 
 * This configuration allows:
 * - Logging all requests/responses for debugging
 * - Centralized error handling
 * - Adding custom headers to all requests
 */
@Configuration
@Slf4j
public class FeignConfig {

    /**
     * Enable detailed logging for Feign calls.
     * Useful for debugging and monitoring in production.
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    /**
     * Interceptor to add custom headers to all requests.
     * Example: correlation ID for distributed tracing.
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            requestTemplate.header("X-Request-Source", "user-service");
            log.debug("Feign request to: {} {}", requestTemplate.method(), requestTemplate.url());
        };
    }

    /**
     * Custom error decoder for handling HTTP errors.
     * Allows mapping specific errors to custom exceptions.
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return (methodKey, response) -> {
            log.error("Feign error for method {}: status {} - {}", 
                methodKey, response.status(), response.reason());
            
            if (response.status() >= 500) {
                return new RuntimeException("Notification service unavailable: " + response.reason());
            } else if (response.status() == 404) {
                return new RuntimeException("Notification service endpoint not found");
            } else {
                return new RuntimeException("Notification service error: " + response.reason());
            }
        };
    }
}

