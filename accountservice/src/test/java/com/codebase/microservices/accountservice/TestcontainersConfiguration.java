package com.codebase.microservices.accountservice;

import com.codebase.microservices.accountservice.controllers.AccountController;
import com.codebase.microservices.accountservice.controllers.AuthController;
import com.codebase.microservices.accountservice.controllers.HealthController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	KafkaContainer kafkaContainer() {
		return new KafkaContainer(DockerImageName.parse("apache/kafka-native:latest"));
	}

	@Bean
	@ServiceConnection
	MySQLContainer<?> mysqlContainer() {
		return new MySQLContainer<>(DockerImageName.parse("mysql:latest"));
	}

}





@WebMvcTest(AuthController.class)
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserService userService;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void registerUser_ShouldReturnSuccess() throws Exception {
		// Given
		RegisterRequest request = new RegisterRequest(
				"testuser",
				"test@example.com",
				"password123",
				"123 Main St",
				"123 Main St"
		);

		ApiResponse mockResponse = ApiResponse.success("User registered successfully");
		when(userService.registerUser(any(RegisterRequest.class))).thenReturn(mockResponse);

		// When & Then
		mockMvc.perform(post("/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("User registered successfully"));
	}

	@Test
	void login_ShouldReturnTokenOnSuccess() throws Exception {
		// Given
		LoginRequest request = new LoginRequest("testuser", "password123");

		UserDto userDto = new UserDto(1L, "testuser", "test@example.com",
				"123 Main St", "123 Main St", "USER", true);
		LoginResponse mockResponse = new LoginResponse("mock-jwt-token", "mock-refresh-token", userDto);

		when(userService.authenticateUser(any(LoginRequest.class))).thenReturn(mockResponse);

		// When & Then
		mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").value("mock-jwt-token"))
				.andExpect(jsonPath("$.type").value("Bearer"))
				.andExpect(jsonPath("$.user.username").value("testuser"));
	}

	@Test
	void logout_ShouldReturnSuccess() throws Exception {
		// When & Then
		mockMvc.perform(post("/auth/logout"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Logged out successfully"));
	}

	@Test
	void register_WithInvalidData_ShouldReturnBadRequest() throws Exception {
		// Given - empty username (should fail validation)
		RegisterRequest request = new RegisterRequest(
				"", // Invalid - empty username
				"invalid-email", // Invalid email format
				"123", // Invalid - too short password
				"",
				""
		);

		// When & Then
		mockMvc.perform(post("/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}
}

// Second test class for AccountController
@WebMvcTest(AccountController.class)
class AccountControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserService userService;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void createAccount_ShouldReturnSuccess() throws Exception {
		// Given
		RegisterRequest request = new RegisterRequest(
				"testuser2",
				"test2@example.com",
				"password123",
				"456 Oak St",
				"456 Oak St"
		);

		ApiResponse mockResponse = ApiResponse.success("User registered successfully");
		when(userService.registerUser(any(RegisterRequest.class))).thenReturn(mockResponse);

		// When & Then
		mockMvc.perform(post("/accounts")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true));
	}
}

// Minimal Health Controller Test
@WebMvcTest(HealthController.class)
class HealthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void healthCheck_ShouldReturnUp() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.service").value("account-service"));
	}
}
