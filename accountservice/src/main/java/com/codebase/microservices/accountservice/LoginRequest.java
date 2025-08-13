package com.codebase.microservices.accountservice;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

// Login Request DTO
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;
}
