package com.example.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Cloud Gateway Application
 * 
 * This is a minimal Gateway service that acts as a single entry point
 * for all client requests. It uses Consul for service discovery to
 * dynamically route requests to backend services.
 * 
 * Key features:
 * - Routes requests to user-service via Consul discovery
 * - Load balances across multiple service instances
 * - Registers itself in Consul for monitoring
 * 
 * No custom code needed - all routing configuration is in application.yml
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}

