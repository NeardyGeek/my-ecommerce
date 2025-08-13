package com.codebase.microservices.accountservice.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/actuator")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"})
public class HealthController implements HealthIndicator {

    private final DataSource dataSource;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> healthInfo = new HashMap<>();

        try {
            // Check database connectivity
            try (Connection connection = dataSource.getConnection()) {
                if (connection.isValid(2)) {
                    healthInfo.put("status", "UP");
                    healthInfo.put("database", "UP");
                } else {
                    healthInfo.put("status", "DOWN");
                    healthInfo.put("database", "DOWN");
                }
            }
        } catch (Exception e) {
            log.error("Health check failed", e);
            healthInfo.put("status", "DOWN");
            healthInfo.put("database", "DOWN");
            healthInfo.put("error", e.getMessage());
        }

        healthInfo.put("service", "account-service");
        healthInfo.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(healthInfo);
    }

    @Override
    public Health health() {
        try {
            try (Connection connection = dataSource.getConnection()) {
                if (connection.isValid(2)) {
                    return Health.up()
                            .withDetail("database", "UP")
                            .withDetail("service", "account-service")
                            .build();
                } else {
                    return Health.down()
                            .withDetail("database", "DOWN")
                            .withDetail("service", "account-service")
                            .build();
                }
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("database", "DOWN")
                    .withDetail("error", e.getMessage())
                    .withDetail("service", "account-service")
                    .build();
        }
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("app", "Account Service");
        info.put("version", "1.0.0");
        info.put("description", "Account management microservice");
        info.put("profiles", System.getProperty("spring.profiles.active", "default"));

        return ResponseEntity.ok(info);
    }
}