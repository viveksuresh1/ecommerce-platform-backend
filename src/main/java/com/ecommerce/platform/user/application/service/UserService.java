package com.ecommerce.platform.user.application.service;

import com.ecommerce.platform.shared.exception.BadRequestException;
import com.ecommerce.platform.shared.exception.ResourceNotFoundException;
import com.ecommerce.platform.user.api.dto.ChangePasswordRequest;
import com.ecommerce.platform.user.api.dto.UpdateProfileRequest;
import com.ecommerce.platform.user.api.dto.UserProfileResponse;
import com.ecommerce.platform.user.api.mapper.UserMapper;
import com.ecommerce.platform.user.domain.model.User;
import com.ecommerce.platform.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for user profile operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * Get user profile by ID.
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        User user = findUserById(userId);
        return userMapper.toProfileResponse(user);
    }

    /**
     * Update user profile.
     */
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUserById(userId);

        // Update only provided fields
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        user = userRepository.save(user);
        log.info("User profile updated: {}", user.getEmail());

        return userMapper.toProfileResponse(user);
    }

    /**
     * Change user password.
     */
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUserById(userId);

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        // Check new password is different
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("New password must be different from current password");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed for user: {}", user.getEmail());
    }

    /**
     * Find user by ID or throw exception.
     */
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }
}
