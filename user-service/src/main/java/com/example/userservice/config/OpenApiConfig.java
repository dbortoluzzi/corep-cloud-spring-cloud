package com.example.userservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration.
 * 
 * Provides API documentation accessible at:
 * - Swagger UI: http://localhost:8081/swagger-ui/index.html
 * - API Docs: http://localhost:8081/v3/api-docs
 */
@Configuration
@Slf4j
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        log.info("Configuring OpenAPI/Swagger documentation");
        return new OpenAPI()
            .info(new Info()
                .title("User Service API")
                .version("1.0.0")
                .description("REST API for User management with OpenFeign and Resilience4j")
                .contact(new Contact()
                    .name("User Service Team")
                    .email("support@example.com"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        log.info("Configuring GroupedOpenApi for all endpoints");
        return GroupedOpenApi.builder()
            .group("user-service")
            .pathsToMatch("/**")
            .build();
    }
}

