package com.example.userservice.client;

import com.example.userservice.dto.NotificationRequest;
import com.example.userservice.dto.NotificationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Feign client for Notification Service.
 * 
 * OpenFeign allows declarative REST client definition:
 * - No boilerplate HTTP code needed
 * - Integrates with service discovery (Consul)
 * - Automatic serialization/deserialization
 * 
 * In a cloud environment, this client can automatically resolve
 * the service URL via Consul service discovery.
 */
@FeignClient(
    name = "notification-service",
    url = "${external.notification-service.url:http://localhost:9090}",
    configuration = com.example.userservice.config.FeignConfig.class
)
public interface NotificationClient {
    
    /**
     * Send a notification.
     * This call is protected by circuit breaker, retry, and timeout
     * configured in Resilience4jConfig.
     */
    @PostMapping("/api/notifications/send")
    NotificationResponse sendNotification(NotificationRequest request);
}

