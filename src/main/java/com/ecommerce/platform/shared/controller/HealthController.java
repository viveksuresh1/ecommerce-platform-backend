package com.ecommerce.platform.shared.controller;

import com.ecommerce.platform.shared.dto.ApiResponse;
import com.ecommerce.platform.shared.exception.BadRequestException;
import com.ecommerce.platform.shared.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Test controller to verify infrastructure is working.
 * Can be removed in production.
 */
@RestController
@RequestMapping("/api/v1/test")
@Tag(name = "Test", description = "Test endpoints for verifying infrastructure")
public class HealthController {

    @GetMapping("/ping")
    @Operation(summary = "Simple ping test")
    public ApiResponse<Map<String, String>> ping() {
        return ApiResponse.success(Map.of(
            "message", "pong",
            "status", "API is working"
        ));
    }

    @GetMapping("/error/not-found")
    @Operation(summary = "Test 404 error response")
    public ApiResponse<Void> testNotFound() {
        throw new ResourceNotFoundException("User", "id", 999);
    }

    @GetMapping("/error/bad-request")
    @Operation(summary = "Test 400 error response")
    public ApiResponse<Void> testBadRequest() {
        throw new BadRequestException("This is a test bad request error");
    }

    @GetMapping("/error/unexpected")
    @Operation(summary = "Test 500 error response")
    public ApiResponse<Void> testUnexpectedError() {
        throw new RuntimeException("This is a test unexpected error");
    }
}
