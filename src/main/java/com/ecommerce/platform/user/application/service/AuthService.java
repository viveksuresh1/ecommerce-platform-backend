package com.ecommerce.platform.user.application.service;

import com.ecommerce.platform.shared.exception.BadRequestException;
import com.ecommerce.platform.shared.exception.DuplicateResourceException;
import com.ecommerce.platform.shared.exception.UnauthorizedException;
import com.ecommerce.platform.shared.security.CustomUserDetails;
import com.ecommerce.platform.shared.security.JwtService;
import com.ecommerce.platform.user.api.dto.AuthResponse;
import com.ecommerce.platform.user.api.dto.LoginRequest;
import com.ecommerce.platform.user.api.dto.RefreshTokenRequest;
import com.ecommerce.platform.user.api.dto.RegisterRequest;
import com.ecommerce.platform.user.domain.model.Role;
import com.ecommerce.platform.user.domain.model.User;
import com.ecommerce.platform.user.domain.repository.RoleRepository;
import com.ecommerce.platform.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service handling authentication operations:
 * - User registration
 * - Login (returns JWT tokens)
 * - Token refresh
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Value("${app.jwt.access-token-expiration}")
    private long accessTokenExpiration;

    /**
     * Register a new user.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        // Get default CUSTOMER role
        Role customerRole = roleRepository.findByName(Role.CUSTOMER)
                .orElseThrow(() -> new BadRequestException("Default role not found"));

        // Create new user
        User user = User.builder()
                .email(request.getEmail().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .build();

        user.addRole(customerRole);

        // Save user
        user = userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());

        // Generate tokens and return response
        return generateAuthResponse(user);
    }

    /**
     * Authenticate user and return tokens.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        try {
            // Authenticate with Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail().toLowerCase(),
                            request.getPassword()
                    )
            );

            // Get user details
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            // Load full user entity for response
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new UnauthorizedException("User not found"));

            log.info("User logged in: {}", user.getEmail());

            return generateAuthResponse(user);

        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for email: {}", request.getEmail());
            throw new UnauthorizedException("Invalid email or password");
        }
    }

    /**
     * Refresh access token using refresh token.
     */
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        // Validate refresh token
        if (!jwtService.isTokenValid(refreshToken)) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        // Extract email from token
        String email = jwtService.extractUsername(refreshToken);

        // Load user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        log.info("Token refreshed for user: {}", user.getEmail());

        return generateAuthResponse(user);
    }

    /**
     * Generate auth response with tokens and user info.
     */
    private AuthResponse generateAuthResponse(User user) {
        CustomUserDetails userDetails = new CustomUserDetails(user);

        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration / 1000) // Convert ms to seconds
                .user(AuthResponse.UserResponse.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .fullName(user.getFullName())
                        .build())
                .build();
    }
}
