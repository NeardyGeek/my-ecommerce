package com.codebase.microservices.accountservice;

import lombok.*;

// Login Response DTO
@Data
@NoArgsConstructor
@Getter
@Setter
public class LoginResponse {
    private String token;
    private String refreshToken;
    private String type;
    private UserDto user;

    public LoginResponse(String token, String refreshToken, UserDto user) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.type = "Bearer";
        this.user = user;
    }
}
