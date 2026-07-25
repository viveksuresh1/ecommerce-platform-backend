package com.ecommerce.platform.admin.api.controller;

import com.ecommerce.platform.admin.api.dto.*;
import com.ecommerce.platform.admin.application.service.AdminService;
import com.ecommerce.platform.shared.dto.ApiResponse;
import com.ecommerce.platform.shared.dto.PagedResponse;
import com.ecommerce.platform.user.domain.model.UserStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin management endpoints")
public class AdminController {

    private final AdminService adminService;

    // ==================== Dashboard ====================

    @GetMapping("/dashboard")
    @Operation(summary = "Get dashboard statistics")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats() {
        DashboardStatsResponse stats = adminService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    // ==================== User Management ====================

    @GetMapping("/users")
    @Operation(summary = "Get all users")
    public ResponseEntity<ApiResponse<PagedResponse<UserManagementResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<UserManagementResponse> users = adminService.getAllUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/users/status/{status}")
    @Operation(summary = "Get users by status")
    public ResponseEntity<ApiResponse<PagedResponse<UserManagementResponse>>> getUsersByStatus(
            @PathVariable UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<UserManagementResponse> users = adminService.getUsersByStatus(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/users/search")
    @Operation(summary = "Search users")
    public ResponseEntity<ApiResponse<PagedResponse<UserManagementResponse>>> searchUsers(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<UserManagementResponse> users = adminService.searchUsers(query, pageable);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get user details")
    public ResponseEntity<ApiResponse<UserManagementResponse>> getUserById(@PathVariable Long userId) {
        UserManagementResponse user = adminService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PutMapping("/users/{userId}/status")
    @Operation(summary = "Update user status")
    public ResponseEntity<ApiResponse<UserManagementResponse>> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        UserManagementResponse user = adminService.updateUserStatus(userId, request);
        return ResponseEntity.ok(ApiResponse.success(user, "User status updated"));
    }

    @PostMapping("/users/{userId}/roles")
    @Operation(summary = "Add role to user")
    public ResponseEntity<ApiResponse<UserManagementResponse>> addUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRoleRequest request) {
        UserManagementResponse user = adminService.addUserRole(userId, request);
        return ResponseEntity.ok(ApiResponse.success(user, "Role added to user"));
    }

    @DeleteMapping("/users/{userId}/roles/{roleName}")
    @Operation(summary = "Remove role from user")
    public ResponseEntity<ApiResponse<UserManagementResponse>> removeUserRole(
            @PathVariable Long userId,
            @PathVariable String roleName) {
        UserManagementResponse user = adminService.removeUserRole(userId, roleName);
        return ResponseEntity.ok(ApiResponse.success(user, "Role removed from user"));
    }
}
