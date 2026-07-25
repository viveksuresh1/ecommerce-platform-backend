package com.ecommerce.platform.user.api.controller;

import com.ecommerce.platform.shared.dto.ApiResponse;
import com.ecommerce.platform.user.api.dto.AuthResponse;
import com.ecommerce.platform.user.api.dto.LoginRequest;
import com.ecommerce.platform.user.api.dto.RefreshTokenRequest;
import com.ecommerce.platform.user.api.dto.RegisterRequest;
import com.ecommerce.platform.user.application.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints for user registration, login, and token refresh.
 * All endpoints are public (no authentication required).
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User registration, login, and token management")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new user", description = "Creates a new user account and returns authentication tokens")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ApiResponse.success(response, "User registered successfully");
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticates user and returns access and refresh tokens")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ApiResponse.success(response, "Login successful");
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Returns new access token using a valid refresh token")
    public ApiResponse<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ApiResponse.success(response, "Token refreshed successfully");
    }
}
