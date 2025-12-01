package com.example.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "User data transfer object")
public class UserDTO {
    
    @Schema(description = "User unique identifier", example = "1")
    private Long id;
    
    @Schema(description = "User email address", example = "john@example.com")
    private String email;
    
    @Schema(description = "User full name", example = "John Doe")
    private String name;
    
    @Schema(description = "User creation timestamp", example = "2025-12-01T10:00:00")
    private LocalDateTime createdAt;
    
    @Schema(description = "User last update timestamp", example = "2025-12-01T10:00:00", nullable = true)
    private LocalDateTime updatedAt;
}

