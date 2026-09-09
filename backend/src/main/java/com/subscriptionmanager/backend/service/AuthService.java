package com.subscriptionmanager.backend.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.subscriptionmanager.backend.dto.auth.AuthResponse;
import com.subscriptionmanager.backend.dto.auth.LoginRequest;
import com.subscriptionmanager.backend.dto.auth.RegisterRequest;
import com.subscriptionmanager.backend.dto.auth.UserResponse;
import com.subscriptionmanager.backend.entity.PasswordResetToken;
import com.subscriptionmanager.backend.entity.RefreshToken;
import com.subscriptionmanager.backend.entity.User;
import com.subscriptionmanager.backend.entity.UserPreferences;
import com.subscriptionmanager.backend.exception.EmailAlreadyInUseException;
import com.subscriptionmanager.backend.exception.InvalidCurrentPasswordException;
import com.subscriptionmanager.backend.exception.InvalidTokenException;
import com.subscriptionmanager.backend.repository.PasswordResetTokenRepository;
import com.subscriptionmanager.backend.repository.RefreshTokenRepository;
import com.subscriptionmanager.backend.repository.UserPreferencesRepository;
import com.subscriptionmanager.backend.repository.UserRepository;
import com.subscriptionmanager.backend.security.JwtService;
import com.subscriptionmanager.backend.security.OpaqueTokenGenerator;
import com.subscriptionmanager.backend.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final OpaqueTokenGenerator opaqueTokenGenerator;

    @Value("${jwt.refresh-token-ttl-days}")
    private long refreshTokenTtlDays;

    @Value("${app.password-reset.ttl-minutes}")
    private long passwordResetTtlMinutes;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyInUseException(request.email());
        }

        User user = new User();
        user.setEmail(request.email());
        user.setFullName(request.fullName());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user = userRepository.save(user);

        UserPreferences preferences = new UserPreferences();
        preferences.setUser(user);
        userPreferencesRepository.save(preferences);

        return issueTokenPair(user);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + request.email()));

        return issueTokenPair(user);
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        String tokenHash = opaqueTokenGenerator.hash(rawRefreshToken);
        RefreshToken existing = refreshTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (!existing.isActive()) {
            throw new InvalidTokenException("Refresh token is expired or has been revoked");
        }

        existing.setRevokedAt(Instant.now());
        refreshTokenRepository.save(existing);

        return issueTokenPair(existing.getUser());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        String tokenHash = opaqueTokenGenerator.hash(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
        });
    }

    public UserResponse currentUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalStateException("Authenticated user not found: id " + userId));
        return UserResponse.from(user);
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalStateException("Authenticated user not found: id " + userId));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new InvalidCurrentPasswordException();
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        refreshTokenRepository.revokeAllForUser(userId, Instant.now());
    }

    /**
     * Always succeeds from the caller's point of view (no user-enumeration
     * signal). Since no mail server is wired up yet, the reset link is
     * logged rather than emailed.
     */
    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String rawToken = opaqueTokenGenerator.generate();

            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setUser(user);
            resetToken.setTokenHash(opaqueTokenGenerator.hash(rawToken));
            resetToken.setExpiresAt(Instant.now().plus(passwordResetTtlMinutes, ChronoUnit.MINUTES));
            passwordResetTokenRepository.save(resetToken);

            log.info("Password reset requested for {}. Reset token (send via email in production): {}",
                email, rawToken);
        });
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        String tokenHash = opaqueTokenGenerator.hash(rawToken);
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new InvalidTokenException("Invalid or expired reset token"));

        if (!resetToken.isActive()) {
            throw new InvalidTokenException("Invalid or expired reset token");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(resetToken);

        refreshTokenRepository.revokeAllForUser(user.getId(), Instant.now());
    }

    private AuthResponse issueTokenPair(User user) {
        UserPrincipal principal = new UserPrincipal(user);
        String accessToken = jwtService.generateAccessToken(principal);

        String rawRefreshToken = opaqueTokenGenerator.generate();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(opaqueTokenGenerator.hash(rawRefreshToken));
        refreshToken.setExpiresAt(Instant.now().plus(refreshTokenTtlDays, ChronoUnit.DAYS));
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.of(accessToken, rawRefreshToken, jwtService.getAccessTokenTtlSeconds(), UserResponse.from(user));
    }
}
