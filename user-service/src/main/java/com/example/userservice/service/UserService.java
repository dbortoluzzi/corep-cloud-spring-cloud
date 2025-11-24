package com.example.userservice.service;

import com.example.userservice.dto.CreateUserRequest;
import com.example.userservice.dto.NotificationRequest;
import com.example.userservice.dto.NotificationResponse;
import com.example.userservice.dto.UserDTO;
import com.example.userservice.entity.User;
import com.example.userservice.exception.ResourceNotFoundException;
import com.example.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * User Service for managing users.
 * 
 * This service demonstrates integration with external services using:
 * - OpenFeign for declarative REST client calls
 * - Resilience4j for circuit breaker, retry, and timeout patterns
 * 
 * In a cloud environment, this pattern is essential because:
 * - Services can be unstable
 * - Network can have variable latency
 * - Failures must be handled gracefully with fallback
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * Create a new user and send welcome notification.
     * 
     * This method demonstrates Resilience4j usage:
     * - @CircuitBreaker: Opens circuit if notification service is down
     * - @Retry: Automatically retries on transient errors
     * - @TimeLimiter: Sets timeout to avoid indefinite waits
     * 
     * If notification fails, fallback method is called
     * which creates user without notification.
     * 
     * Note: Validation is done BEFORE applying retry/circuit breaker
     * to avoid retrying on validation errors.
     */
    @Transactional
    public CompletableFuture<UserDTO> createUser(CreateUserRequest request) {
        log.info("Creating user: {}", request.getEmail());

        // Check if user already exists (validation BEFORE retry/circuit breaker)
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("User with email " + request.getEmail() + " already exists");
        }

        // Create user
        User user = User.builder()
            .email(request.getEmail())
            .name(request.getName())
            .build();

        user = userRepository.save(user);
        log.info("User created successfully with ID: {}", user.getId());

        // Send notification via Feign client
        // This call is protected by circuit breaker, retry, and timeout
        return sendNotificationWithResilience(user);
    }

    /**
     * Sends notification with Resilience4j patterns.
     * Uses separate NotificationService bean so AOP can intercept retry/circuit breaker.
     */
    private CompletableFuture<UserDTO> sendNotificationWithResilience(User user) {
        NotificationRequest notificationRequest = NotificationRequest.builder()
            .recipient(user.getEmail())
            .message("Welcome " + user.getName() + "! Your account has been created.")
            .build();

        log.info("📧 Sending welcome notification for user: {}", user.getEmail());
        log.info("Notification request: recipient={}, message={}", 
            notificationRequest.getRecipient(), notificationRequest.getMessage());
        
        try {
            // Call via separate bean - retry/circuit breaker will work here
            NotificationResponse notificationResponse = notificationService.sendNotification(notificationRequest);
            log.info("✅ Notification sent successfully: status = {}, notificationId = {}, sentAt = {}", 
                notificationResponse.getStatus(), notificationResponse.getNotificationId(), notificationResponse.getSentAt());
        } catch (Exception e) {
            log.warn("⚠️ Notification service unavailable, using fallback. Error: {}", e.getMessage());
            log.warn("Fallback: User was created but notification was not sent");
            // Continue without notification (graceful degradation)
        }

        return CompletableFuture.completedFuture(toDTO(user));
    }


    /**
     * Get user by ID.
     */
    public UserDTO getUserById(Long id) {
        log.debug("Fetching user with ID: {}", id);
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
        return toDTO(user);
    }

    /**
     * Get user by email.
     */
    public UserDTO getUserByEmail(String email) {
        log.debug("Fetching user with email: {}", email);
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return toDTO(user);
    }

    /**
     * Get all users.
     */
    public List<UserDTO> getAllUsers() {
        log.debug("Fetching all users");
        return userRepository.findAll().stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * Convert Entity to DTO.
     */
    private UserDTO toDTO(User user) {
        return UserDTO.builder()
            .id(user.getId())
            .email(user.getEmail())
            .name(user.getName())
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .build();
    }
}

