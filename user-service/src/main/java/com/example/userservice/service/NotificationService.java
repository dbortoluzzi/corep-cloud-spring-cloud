package com.example.userservice.service;

import com.example.userservice.client.NotificationClient;
import com.example.userservice.dto.NotificationRequest;
import com.example.userservice.dto.NotificationResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Separate service for notification calls with Resilience4j patterns.
 * 
 * This is a separate bean so that Spring AOP can properly intercept
 * the method calls for retry and circuit breaker.
 * 
 * Without a separate bean, self-invocation prevents AOP from working.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationClient notificationClient;

    /**
     * Sends notification with retry and circuit breaker.
     * 
     * IMPORTANT: Resilience4j executes aspects in this order (by default):
     * 1. Circuit Breaker (order 1) - Executes FIRST
     * 2. Retry (order 2) - Executes AFTER Circuit Breaker
     * 
     * This order is configured in application.yml:
     * - circuit-breaker-aspect-order: 1
     * - retry-aspect-order: 2
     * 
     * Execution flow:
     * 1. Circuit Breaker checks state FIRST
     *    - If OPEN: blocks immediately, throws CallNotPermittedException, goes to fallback (NO retry)
     *    - If CLOSED: allows call to proceed to Retry
     * 2. Retry executes AFTER Circuit Breaker (only if CB is CLOSED)
     *    - Makes 3 attempts with fixed interval (500ms between attempts)
     *    - If all retries fail, Circuit Breaker records the failure
     * 3. After N failures, Circuit Breaker opens
     * 
     * When Circuit Breaker is OPEN:
     * - Calls are blocked immediately (no retry)
     * - CallNotPermittedException is thrown
     * - Retry is configured to ignore CallNotPermittedException (in application.yml)
     * - Fallback is called directly
     * 
     * When Circuit Breaker is CLOSED:
     * - Retry attempts 3 times with fixed interval (500ms between attempts)
     * - If all retries fail, Circuit Breaker records the failure
     * - After N failures, Circuit Breaker opens
     * 
     * This method is in a separate bean so Spring AOP can intercept it.
     * You'll see retry logs: 🔄 RETRY #1, 🔄 RETRY #2, 🔄 RETRY #3
     */
    @CircuitBreaker(name = "notification-service", fallbackMethod = "fallbackNotification")
    @Retry(name = "notification-service")
    public NotificationResponse sendNotification(NotificationRequest request) {
        log.info("📧 Calling notification service");
        return notificationClient.sendNotification(request);
    }

    /**
     * Fallback when circuit breaker is open or all retries failed.
     * 
     * This is called when:
     * 1. Circuit Breaker is OPEN (calls blocked immediately, no retry)
     * 2. All retry attempts exhausted (Circuit Breaker is CLOSED but retry failed)
     * 
     * IMPORTANT: We re-throw the original exception so Circuit Breaker can record it correctly.
     * If we throw a new RuntimeException, Circuit Breaker might not record it as a failure.
     */
    public NotificationResponse fallbackNotification(NotificationRequest request, Exception ex) {
        // Check if Circuit Breaker is OPEN (call was blocked immediately)
        if (ex != null && ex.getClass().getSimpleName().contains("CallNotPermittedException")) {
            log.warn("🚫 Circuit breaker is OPEN - Call blocked immediately (no retry)");
        } else {
            log.warn("⚠️ All retry attempts failed - Using fallback. Error: {}", ex != null ? ex.getMessage() : "unknown");
        }
        // Re-throw original exception so Circuit Breaker can record it correctly
        if (ex instanceof RuntimeException) {
            throw (RuntimeException) ex;
        } else if (ex != null) {
            throw new RuntimeException("Notification service unavailable", ex);
        } else {
            throw new RuntimeException("Notification service unavailable");
        }
    }
}

