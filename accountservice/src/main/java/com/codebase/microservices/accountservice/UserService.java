package com.codebase.microservices.accountservice;

import jakarta.ws.rs.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public ApiResponse registerUser(RegisterRequest request) {
        log.info("Registering new user: {}", request.getUsername());

        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username is already taken");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }

        // Create new user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setShippingAddress(request.getShippingAddress());
        user.setBillingAddress(request.getBillingAddress());
        user.setRole(User.Role.USER);
        user.setEnabled(true);

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getUsername());

        return ApiResponse.success("User registered successfully", mapToUserDto(savedUser));
    }

    public LoginResponse authenticateUser(LoginRequest request) {
        log.info("Authenticating user: {}", request.getUsername());

        User user = userRepository.findByUsernameOrEmail(request.getUsername(), request.getUsername())
                .orElseThrow(() -> new BadRequestException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadRequestException("Invalid username or password");
        }

        if (!user.getEnabled()) {
            throw new BadRequestException("Account is disabled");
        }

        // Generate tokens
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().toString());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        log.info("User authenticated successfully: {}", user.getUsername());

        return new LoginResponse(token, refreshToken, mapToUserDto(user));
    }

    @Transactional(readOnly = true)
    public UserDto getUserProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return mapToUserDto(user);
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(this::mapToUserDto)
                .collect(Collectors.toList());
    }

    public ApiResponse updateUserProfile(String username, RegisterRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check if new email is already taken by another user
        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }

        user.setEmail(request.getEmail());
        user.setShippingAddress(request.getShippingAddress());
        user.setBillingAddress(request.getBillingAddress());

        User updatedUser = userRepository.save(user);
        log.info("User profile updated: {}", updatedUser.getUsername());

        return ApiResponse.success("Profile updated successfully", mapToUserDto(updatedUser));
    }

    public ApiResponse deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        userRepository.delete(user);
        log.info("User deleted: {}", user.getUsername());

        return ApiResponse.success("User deleted successfully");
    }

    private UserDto mapToUserDto(User user) {
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getShippingAddress(),
                user.getBillingAddress(),
                user.getRole().toString(),
                user.getEnabled()
        );
    }
}
