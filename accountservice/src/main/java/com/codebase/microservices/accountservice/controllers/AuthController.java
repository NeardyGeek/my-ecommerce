package com.codebase.microservices.accountservice.controllers;

import com.codebase.microservices.accountservice.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"})
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request received for username: {}", request.getUsername());

        ApiResponse response = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request received for username: {}", request.getUsername());

        LoginResponse response = userService.authenticateUser(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout() {
        // In a stateless JWT implementation, logout is typically handled client-side
        // by removing the token from storage. Here we just return a success message.
        log.info("Logout request received");
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }
}


