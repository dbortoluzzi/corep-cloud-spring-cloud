package com.example.userservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

import java.time.Duration;

/**
 * Resilience4j configuration for resilience patterns.
 * 
 * Resilience4j provides essential patterns for cloud microservices:
 * 
 * 1. Circuit Breaker: Prevents calls to down services
 *    - After N failures, stops calling for a period
 *    - Reduces load on unstable services
 * 
 * 2. Retry: Automatically retries failed calls
 *    - Useful for transient errors (network, timeout)
 *    - Fixed interval between retries
 * 
 * 3. Time Limiter: Sets timeout for async operations
 *    - Prevents indefinite waits
 *    - Improves application responsiveness
 * 
 * These patterns are essential in cloud environments where:
 * - Services can be unstable
 * - Network can have variable latency
 * - Failures are common and must be handled gracefully
 */
@Configuration
@Slf4j
public class Resilience4jConfig {

    @Autowired
    private RetryRegistry retryRegistry;
    
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    /**
     * Register event listeners on the retry that is actually used by annotations.
     * This must be done after Spring creates the retry from the bean.
     */
    @PostConstruct
    public void setupRetryEventListeners() {
        // Get the retry that will be used by @Retry annotation
        Retry retry = retryRegistry.retry("notification-service");
        
        retry.getEventPublisher()
            .onRetry(event -> 
                log.warn("🔄 RETRY #{} - Retrying notification service call. Error: {}", 
                    event.getNumberOfRetryAttempts(), 
                    event.getLastThrowable() != null ? event.getLastThrowable().getClass().getSimpleName() : "unknown"));
        
        retry.getEventPublisher()
            .onError(event -> 
                log.error("❌ RETRY FAILED - All retry attempts exhausted. Error: {}", 
                    event.getLastThrowable() != null ? event.getLastThrowable().getMessage() : "unknown"));
        
        retry.getEventPublisher()
            .onSuccess(event -> 
                log.info("✅ RETRY SUCCESS - Notification sent after {} attempts", 
                    event.getNumberOfRetryAttempts()));
        
        log.info("✅ Retry event listeners registered for 'notification-service'");
        
        // Register Circuit Breaker event listeners
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("notification-service");
        log.info("✅ Circuit breaker 'notification-service' found. Current state: {}", circuitBreaker.getState());
        
        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> 
                log.info("🔌 Circuit breaker state transition: {} -> {}", 
                    event.getStateTransition().getFromState(), 
                    event.getStateTransition().getToState()));
        
        circuitBreaker.getEventPublisher()
            .onCallNotPermitted(event -> 
                log.warn("🚫 Circuit breaker is OPEN - Call not permitted. State: {}", circuitBreaker.getState()));
        
        circuitBreaker.getEventPublisher()
            .onFailureRateExceeded(event -> 
                log.warn("⚠️ Circuit breaker failure rate exceeded: {}%", event.getFailureRate()));
        
        log.info("✅ Circuit breaker event listeners registered for 'notification-service'");
    }

    /**
     * NOTE: Retry and Circuit Breaker configurations are in application.yml.
     * Spring Boot auto-configures them from YAML.
     * This class only registers event listeners for logging purposes.
     */

    /**
     * Time Limiter for notification service.
     * 
     * Behavior:
     * - Timeout of 3 seconds
     * - Cancels running operations if timeout exceeded
     */
    @Bean
    public TimeLimiter notificationTimeLimiter() {
        return TimeLimiter.of("notification-service", 
            TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(3))  // 3 second timeout
                .cancelRunningFuture(true)  // Cancel running operations
                .build());
    }
}

