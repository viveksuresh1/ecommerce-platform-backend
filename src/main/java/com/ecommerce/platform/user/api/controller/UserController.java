package com.ecommerce.platform.user.api.controller;

import com.ecommerce.platform.shared.dto.ApiResponse;
import com.ecommerce.platform.shared.security.CustomUserDetails;
import com.ecommerce.platform.user.api.dto.AddressResponse;
import com.ecommerce.platform.user.api.dto.ChangePasswordRequest;
import com.ecommerce.platform.user.api.dto.CreateAddressRequest;
import com.ecommerce.platform.user.api.dto.UpdateAddressRequest;
import com.ecommerce.platform.user.api.dto.UpdateProfileRequest;
import com.ecommerce.platform.user.api.dto.UserProfileResponse;
import com.ecommerce.platform.user.application.service.AddressService;
import com.ecommerce.platform.user.application.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * User profile and address management endpoints.
 * All endpoints require authentication.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "User profile and address management")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;
    private final AddressService addressService;

    // ==================== Profile Endpoints ====================

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ApiResponse<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UserProfileResponse profile = userService.getProfile(userDetails.getId());
        return ApiResponse.success(profile);
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile")
    public ApiResponse<UserProfileResponse> updateMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserProfileResponse profile = userService.updateProfile(userDetails.getId(), request);
        return ApiResponse.success(profile, "Profile updated successfully");
    }

    @PostMapping("/me/change-password")
    @Operation(summary = "Change password")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userDetails.getId(), request);
        return ApiResponse.success(null, "Password changed successfully");
    }

    // ==================== Address Endpoints ====================

    @GetMapping("/me/addresses")
    @Operation(summary = "Get all addresses for current user")
    public ApiResponse<List<AddressResponse>> getMyAddresses(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<AddressResponse> addresses = addressService.getUserAddresses(userDetails.getId());
        return ApiResponse.success(addresses);
    }

    @GetMapping("/me/addresses/{addressId}")
    @Operation(summary = "Get a specific address")
    public ApiResponse<AddressResponse> getAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long addressId) {
        AddressResponse address = addressService.getAddress(userDetails.getId(), addressId);
        return ApiResponse.success(address);
    }

    @PostMapping("/me/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a new address")
    public ApiResponse<AddressResponse> createAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateAddressRequest request) {
        AddressResponse address = addressService.createAddress(userDetails.getId(), request);
        return ApiResponse.success(address, "Address added successfully");
    }

    @PutMapping("/me/addresses/{addressId}")
    @Operation(summary = "Update an address")
    public ApiResponse<AddressResponse> updateAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long addressId,
            @Valid @RequestBody UpdateAddressRequest request) {
        AddressResponse address = addressService.updateAddress(userDetails.getId(), addressId, request);
        return ApiResponse.success(address, "Address updated successfully");
    }

    @DeleteMapping("/me/addresses/{addressId}")
    @Operation(summary = "Delete an address")
    public ApiResponse<Void> deleteAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long addressId) {
        addressService.deleteAddress(userDetails.getId(), addressId);
        return ApiResponse.success(null, "Address deleted successfully");
    }

    @PostMapping("/me/addresses/{addressId}/set-default")
    @Operation(summary = "Set an address as default")
    public ApiResponse<AddressResponse> setDefaultAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long addressId) {
        AddressResponse address = addressService.setDefaultAddress(userDetails.getId(), addressId);
        return ApiResponse.success(address, "Default address updated");
    }
}
