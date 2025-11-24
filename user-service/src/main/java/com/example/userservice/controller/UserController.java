package com.example.userservice.controller;

import com.example.userservice.dto.CreateUserRequest;
import com.example.userservice.dto.UserDTO;
import com.example.userservice.service.UserService;
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
public class UserController {

    private final UserService userService;

    @PostMapping
    public CompletableFuture<ResponseEntity<UserDTO>> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        log.info("POST /users - Creating user: {}", request.getEmail());
        return userService.createUser(request)
            .thenApply(userResponse -> 
                ResponseEntity.status(HttpStatus.CREATED).body(userResponse));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable("id") Long id) {
        log.debug("GET /users/{}", id);
        UserDTO user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserDTO> getUserByEmail(@PathVariable("email") String email) {
        log.debug("GET /users/email/{}", email);
        UserDTO user = userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }

    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        log.debug("GET /users");
        List<UserDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }
}

