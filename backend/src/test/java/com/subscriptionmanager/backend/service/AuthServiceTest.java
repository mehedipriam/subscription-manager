package com.subscriptionmanager.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.subscriptionmanager.backend.dto.auth.AuthResponse;
import com.subscriptionmanager.backend.dto.auth.LoginRequest;
import com.subscriptionmanager.backend.dto.auth.RegisterRequest;
import com.subscriptionmanager.backend.dto.auth.UserResponse;
import com.subscriptionmanager.backend.entity.PasswordResetToken;
import com.subscriptionmanager.backend.entity.RefreshToken;
import com.subscriptionmanager.backend.entity.User;
import com.subscriptionmanager.backend.exception.EmailAlreadyInUseException;
import com.subscriptionmanager.backend.exception.InvalidCurrentPasswordException;
import com.subscriptionmanager.backend.exception.InvalidTokenException;
import com.subscriptionmanager.backend.repository.PasswordResetTokenRepository;
import com.subscriptionmanager.backend.repository.RefreshTokenRepository;
import com.subscriptionmanager.backend.repository.UserPreferencesRepository;
import com.subscriptionmanager.backend.repository.UserRepository;
import com.subscriptionmanager.backend.security.JwtService;
import com.subscriptionmanager.backend.security.OpaqueTokenGenerator;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserPreferencesRepository userPreferencesRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private OpaqueTokenGenerator opaqueTokenGenerator;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(
            userRepository, userPreferencesRepository, refreshTokenRepository, passwordResetTokenRepository,
            passwordEncoder, authenticationManager, jwtService, opaqueTokenGenerator);
        ReflectionTestUtils.setField(service, "refreshTokenTtlDays", 7L);
        ReflectionTestUtils.setField(service, "passwordResetTtlMinutes", 30L);
    }

    private User user(long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setPasswordHash("hashed");
        return user;
    }

    private void stubTokenIssuance() {
        when(jwtService.generateAccessToken(any())).thenReturn("access-token");
        when(jwtService.getAccessTokenTtlSeconds()).thenReturn(900L);
        when(opaqueTokenGenerator.generate()).thenReturn("raw-refresh-token");
        when(opaqueTokenGenerator.hash(anyString())).thenReturn("hashed-refresh-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void registerThrowsWhenEmailAlreadyInUse() {
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new RegisterRequest("Name", "taken@example.com", "password123")))
            .isInstanceOf(EmailAlreadyInUseException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void registerCreatesUserAndPreferencesAndIssuesTokens() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        stubTokenIssuance();

        AuthResponse response = service.register(new RegisterRequest("New User", "new@example.com", "password123"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("raw-refresh-token");
        assertThat(response.user().email()).isEqualTo("new@example.com");
        verify(userPreferencesRepository).save(any());
    }

    @Test
    void loginAuthenticatesAndIssuesTokensForTheUser() {
        User existing = user(1L, "user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(existing));
        stubTokenIssuance();

        AuthResponse response = service.login(new LoginRequest("user@example.com", "password123"));

        assertThat(response.user().id()).isEqualTo(1L);
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void refreshRevokesTheOldTokenAndIssuesANewPair() {
        User existing = user(1L, "user@example.com");
        RefreshToken oldToken = new RefreshToken();
        oldToken.setUser(existing);
        oldToken.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));
        stubTokenIssuance();
        // Override the generic hash() stub for this specific token, registered
        // after stubTokenIssuance() so it wins for that argument (Mockito uses
        // the most-recently-registered matching stub).
        when(opaqueTokenGenerator.hash("raw-old-token")).thenReturn("hashed-old-token");
        when(refreshTokenRepository.findByTokenHash("hashed-old-token")).thenReturn(Optional.of(oldToken));

        service.refresh("raw-old-token");

        assertThat(oldToken.getRevokedAt()).isNotNull();
    }

    @Test
    void refreshThrowsWhenTokenNotFound() {
        when(opaqueTokenGenerator.hash("unknown")).thenReturn("hashed-unknown");
        when(refreshTokenRepository.findByTokenHash("hashed-unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh("unknown")).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void refreshThrowsWhenTokenIsExpired() {
        RefreshToken expired = new RefreshToken();
        expired.setUser(user(1L, "user@example.com"));
        expired.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));
        when(opaqueTokenGenerator.hash("expired")).thenReturn("hashed-expired");
        when(refreshTokenRepository.findByTokenHash("hashed-expired")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.refresh("expired")).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void refreshThrowsWhenTokenAlreadyRevoked() {
        RefreshToken revoked = new RefreshToken();
        revoked.setUser(user(1L, "user@example.com"));
        revoked.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));
        revoked.setRevokedAt(Instant.now().minus(1, ChronoUnit.HOURS));
        when(opaqueTokenGenerator.hash("revoked")).thenReturn("hashed-revoked");
        when(refreshTokenRepository.findByTokenHash("hashed-revoked")).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> service.refresh("revoked")).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void logoutRevokesTheMatchingToken() {
        RefreshToken token = new RefreshToken();
        when(opaqueTokenGenerator.hash("raw")).thenReturn("hashed");
        when(refreshTokenRepository.findByTokenHash("hashed")).thenReturn(Optional.of(token));

        service.logout("raw");

        assertThat(token.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(token);
    }

    @Test
    void logoutIsANoOpWhenTokenDoesNotExist() {
        when(opaqueTokenGenerator.hash("unknown")).thenReturn("hashed-unknown");
        when(refreshTokenRepository.findByTokenHash("hashed-unknown")).thenReturn(Optional.empty());

        service.logout("unknown");

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void currentUserReturnsTheUsersProfile() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "user@example.com")));

        UserResponse response = service.currentUser(1L);

        assertThat(response.email()).isEqualTo("user@example.com");
    }

    @Test
    void currentUserThrowsWhenUserNoLongerExists() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.currentUser(1L)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void changePasswordUpdatesHashAndRevokesAllRefreshTokens() {
        User existing = user(1L, "user@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("oldpass", "hashed")).thenReturn(true);
        when(passwordEncoder.encode("newpass123")).thenReturn("new-hashed");

        service.changePassword(1L, "oldpass", "newpass123");

        assertThat(existing.getPasswordHash()).isEqualTo("new-hashed");
        verify(refreshTokenRepository).revokeAllForUser(eq(1L), any());
    }

    @Test
    void changePasswordThrowsWhenCurrentPasswordIsWrong() {
        User existing = user(1L, "user@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> service.changePassword(1L, "wrong", "newpass123"))
            .isInstanceOf(InvalidCurrentPasswordException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void forgotPasswordCreatesAResetTokenForAnExistingUser() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user(1L, "user@example.com")));
        when(opaqueTokenGenerator.generate()).thenReturn("raw-reset-token");
        when(opaqueTokenGenerator.hash("raw-reset-token")).thenReturn("hashed-reset-token");

        service.forgotPassword("user@example.com");

        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getTokenHash()).isEqualTo("hashed-reset-token");
    }

    @Test
    void forgotPasswordIsANoOpForAnUnknownEmailWithNoErrorSignal() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        service.forgotPassword("nobody@example.com");

        verify(passwordResetTokenRepository, never()).save(any());
    }

    @Test
    void resetPasswordUpdatesPasswordMarksTokenUsedAndRevokesRefreshTokens() {
        User existing = user(1L, "user@example.com");
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(existing);
        token.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        when(opaqueTokenGenerator.hash("raw-token")).thenReturn("hashed-token");
        when(passwordResetTokenRepository.findByTokenHash("hashed-token")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("newpass123")).thenReturn("new-hashed");

        service.resetPassword("raw-token", "newpass123");

        assertThat(existing.getPasswordHash()).isEqualTo("new-hashed");
        assertThat(token.getUsedAt()).isNotNull();
        verify(refreshTokenRepository).revokeAllForUser(eq(1L), any());
    }

    @Test
    void resetPasswordThrowsWhenTokenNotFound() {
        when(opaqueTokenGenerator.hash("unknown")).thenReturn("hashed-unknown");
        when(passwordResetTokenRepository.findByTokenHash("hashed-unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetPassword("unknown", "newpass123"))
            .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void resetPasswordThrowsWhenTokenAlreadyUsed() {
        PasswordResetToken used = new PasswordResetToken();
        used.setUser(user(1L, "user@example.com"));
        used.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        used.setUsedAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        when(opaqueTokenGenerator.hash("used")).thenReturn("hashed-used");
        when(passwordResetTokenRepository.findByTokenHash("hashed-used")).thenReturn(Optional.of(used));

        assertThatThrownBy(() -> service.resetPassword("used", "newpass123"))
            .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void resetPasswordThrowsWhenTokenExpired() {
        PasswordResetToken expired = new PasswordResetToken();
        expired.setUser(user(1L, "user@example.com"));
        expired.setExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        when(opaqueTokenGenerator.hash("expired")).thenReturn("hashed-expired");
        when(passwordResetTokenRepository.findByTokenHash("hashed-expired")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.resetPassword("expired", "newpass123"))
            .isInstanceOf(InvalidTokenException.class);
    }
}
