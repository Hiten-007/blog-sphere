package org.example.blogspherebackend.service.impl;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.example.blogspherebackend.dto.request.LoginRequest;
import org.example.blogspherebackend.dto.request.RefreshTokenRequest;
import org.example.blogspherebackend.dto.request.RegisterRequest;
import org.example.blogspherebackend.dto.response.AuthResponse;
import org.example.blogspherebackend.dto.response.UserResponse;
import org.example.blogspherebackend.entity.User;
import org.example.blogspherebackend.enums.Role;
import org.example.blogspherebackend.exception.BadRequestException;
import org.example.blogspherebackend.exception.UnauthorizedException;
import org.example.blogspherebackend.security.JwtUtil;
import org.example.blogspherebackend.service.AuthService;
import org.example.blogspherebackend.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of AuthService interface.
 * Handles user registration, login, and token refresh operations.
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if email already exists
        if (userService.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        // Create new user with hashed password
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role(Role.SUBSCRIBER)
                .emailVerified(false)
                .isDeleted(false)
                .build();

        // Save user
        user = userService.createUser(user);

        // Generate tokens and return response
        return buildAuthResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // Find user by email
        User user = userService.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        // Generate tokens and return response
        return buildAuthResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        // Validate refresh token
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        // Verify it's a refresh token (not an access token)
        Claims claims = jwtUtil.extractAllClaims(refreshToken);
        String tokenType = claims.get("type", String.class);
        if (!"refresh".equals(tokenType)) {
            throw new UnauthorizedException("Invalid token type");
        }

        // Get user from token
        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        User user = userService.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        // Generate new tokens and return response
        return buildAuthResponse(user);
    }

    /**
     * Build AuthResponse with tokens and user info.
     *
     * @param user the authenticated user
     * @return AuthResponse containing tokens and user info
     */
    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration / 1000) // Convert ms to seconds
                .user(UserResponse.fromEntity(user))
                .build();
    }
}
