package com.example.userservice.controller;

import com.example.userservice.dto.CreateUserRequest;
import com.example.userservice.dto.UserDTO;
import com.example.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for User management.
 * 
 * This controller exposes HTTP endpoints for user operations.
 * Requests come through Gateway which routes to this service
 * via Consul service discovery.
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "APIs for managing users")
public class UserController {

    private final UserService userService;

    @PostMapping
    @Operation(
        summary = "Create a new user",
        description = "Creates a new user with the provided information and sends a welcome notification"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "User created successfully",
            content = @Content(schema = @Schema(implementation = UserDTO.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input data (validation error)"
        )
    })
    public CompletableFuture<ResponseEntity<UserDTO>> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        log.info("POST /users - Creating user: {}", request.getEmail());
        return userService.createUser(request)
            .thenApply(userResponse -> 
                ResponseEntity.status(HttpStatus.CREATED).body(userResponse));
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get user by ID",
        description = "Retrieves a user by their unique identifier"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User found",
            content = @Content(schema = @Schema(implementation = UserDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User not found"
        )
    })
    public ResponseEntity<UserDTO> getUserById(
            @Parameter(description = "User ID", required = true, example = "1")
            @PathVariable("id") Long id) {
        log.debug("GET /users/{}", id);
        UserDTO user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/email/{email}")
    @Operation(
        summary = "Get user by email",
        description = "Retrieves a user by their email address"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User found",
            content = @Content(schema = @Schema(implementation = UserDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User not found"
        )
    })
    public ResponseEntity<UserDTO> getUserByEmail(
            @Parameter(description = "User email address", required = true, example = "john@example.com")
            @PathVariable("email") String email) {
        log.debug("GET /users/email/{}", email);
        UserDTO user = userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }

    @GetMapping
    @Operation(
        summary = "Get all users",
        description = "Retrieves a list of all users in the system"
    )
    @ApiResponse(
        responseCode = "200",
        description = "List of users retrieved successfully",
        content = @Content(schema = @Schema(implementation = UserDTO.class))
    )
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        log.debug("GET /users");
        List<UserDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }
}

