package com.example.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * User Service Application
 * 
 * This service demonstrates Spring Cloud capabilities:
 * - Service Discovery: Registers itself in Consul
 * - OpenFeign: Declarative REST client for calling Notification Service
 * - Resilience4j: Circuit breaker, retry, and timeout patterns
 * 
 * The service manages users and sends notifications via external service.
 */
@SpringBootApplication
@EnableFeignClients
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}

