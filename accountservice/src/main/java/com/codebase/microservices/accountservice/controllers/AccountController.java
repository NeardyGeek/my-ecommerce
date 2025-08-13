package com.codebase.microservices.accountservice.controllers;

import com.codebase.microservices.accountservice.ApiResponse;
import com.codebase.microservices.accountservice.RegisterRequest;
import com.codebase.microservices.accountservice.UserDto;
import com.codebase.microservices.accountservice.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"})
public class AccountController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse> createAccount(@Valid @RequestBody RegisterRequest request) {
        log.info("Account creation request received for username: {}", request.getUsername());

        ApiResponse response = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentAccount(Authentication authentication) {
        String username = authentication.getName();
        log.info("Fetching current account for user: {}", username);

        UserDto userProfile = userService.getUserProfile(username);
        return ResponseEntity.ok(userProfile);
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse> updateCurrentAccount(
            @Valid @RequestBody RegisterRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        log.info("Updating current account for user: {}", username);

        ApiResponse response = userService.updateUserProfile(username, request);
        return ResponseEntity.ok(response);
    }
}
