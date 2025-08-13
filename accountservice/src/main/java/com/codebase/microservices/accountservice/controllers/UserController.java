package com.codebase.microservices.accountservice.controllers;

import com.codebase.microservices.accountservice.ApiResponse;
import com.codebase.microservices.accountservice.RegisterRequest;
import com.codebase.microservices.accountservice.UserDto;
import com.codebase.microservices.accountservice.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"})
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<UserDto> getCurrentUserProfile(Authentication authentication) {
        String username = authentication.getName();
        log.info("Fetching profile for user: {}", username);

        UserDto userProfile = userService.getUserProfile(username);
        return ResponseEntity.ok(userProfile);
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse> updateCurrentUserProfile(
            @Valid @RequestBody RegisterRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        log.info("Updating profile for user: {}", username);

        ApiResponse response = userService.updateUserProfile(username, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        log.info("Fetching all users");

        List<UserDto> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{username}")
    public ResponseEntity<UserDto> getUserByUsername(@PathVariable String username) {
        log.info("Fetching user by username: {}", username);

        UserDto user = userService.getUserProfile(username);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deleteUser(@PathVariable Long userId) {
        log.info("Deleting user with ID: {}", userId);

        ApiResponse response = userService.deleteUser(userId);
        return ResponseEntity.ok(response);
    }
}
