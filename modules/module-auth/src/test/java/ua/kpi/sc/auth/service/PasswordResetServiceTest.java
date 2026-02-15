package ua.kpi.sc.auth.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import ua.kpi.sc.auth.config.PasswordResetProperties;
import ua.kpi.sc.auth.entity.PasswordResetToken;
import ua.kpi.sc.auth.repository.PasswordResetTokenRepository;
import ua.kpi.sc.auth.repository.RefreshTokenRepository;
import ua.kpi.sc.common.audit.AuditPublisher;
import ua.kpi.sc.common.exception.BadRequestException;
import ua.kpi.sc.common.security.CapabilityTier;
import ua.kpi.sc.common.security.UserDetailsPort;
import ua.kpi.sc.common.security.UserPrincipal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserDetailsPort userDetailsPort;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private PasswordResetProperties properties;
    @Mock
    private EmailService emailService;
    @Mock
    private AuditPublisher auditPublisher;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private UserPrincipal testPrincipal() {
        return UserPrincipal.builder()
                .id(USER_ID)
                .email("test@kpi.ua")
                .password("$2a$12$hashed")
                .firstName("Test")
                .lastName("User")
                .tier(CapabilityTier.BASIC)
                .active(true)
                .build();
    }

    private UserPrincipal oauthOnlyPrincipal() {
        return UserPrincipal.builder()
                .id(USER_ID)
                .email("oauth@kpi.ua")
                .password(null)
                .firstName("OAuth")
                .lastName("User")
                .tier(CapabilityTier.BASIC)
                .active(true)
                .build();
    }

    @Nested
    class RequestPasswordResetTests {

        @Test
        void existingUserSendsEmail() {
            when(userDetailsPort.loadByEmail("test@kpi.ua")).thenReturn(Optional.of(testPrincipal()));
            when(properties.getTokenExpiration()).thenReturn(3600000L);
            when(properties.getBaseUrl()).thenReturn("http://localhost:3000");
            when(passwordResetTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            passwordResetService.requestPasswordReset("test@kpi.ua");

            verify(emailService).sendPasswordResetEmail(eq("test@kpi.ua"), anyString());
        }

        @Test
        void nonExistentEmailSilentNoEmailSent() {
            when(userDetailsPort.loadByEmail("unknown@kpi.ua")).thenReturn(Optional.empty());

            passwordResetService.requestPasswordReset("unknown@kpi.ua");

            verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
        }

        @Test
        void oauthOnlyUserSilentNoEmailSent() {
            when(userDetailsPort.loadByEmail("oauth@kpi.ua")).thenReturn(Optional.of(oauthOnlyPrincipal()));

            passwordResetService.requestPasswordReset("oauth@kpi.ua");

            verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
        }

        @Test
        void deletesOldTokensForUser() {
            when(userDetailsPort.loadByEmail("test@kpi.ua")).thenReturn(Optional.of(testPrincipal()));
            when(properties.getTokenExpiration()).thenReturn(3600000L);
            when(properties.getBaseUrl()).thenReturn("http://localhost:3000");
            when(passwordResetTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            passwordResetService.requestPasswordReset("test@kpi.ua");

            verify(passwordResetTokenRepository).deleteByUserId(USER_ID);
        }
    }

    @Nested
    class ResetPasswordTests {

        @Test
        void validTokenUpdatesPasswordAndInvalidatesTokens() {
            String rawToken = "valid-reset-token";
            String tokenHash = AuthService.hashToken(rawToken);
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .id(UUID.randomUUID())
                    .userId(USER_ID)
                    .tokenHash(tokenHash)
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();

            when(passwordResetTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(resetToken));
            when(passwordEncoder.encode("newPassword123")).thenReturn("$2a$12$newHash");

            passwordResetService.resetPassword(rawToken, "newPassword123");

            verify(userDetailsPort).updatePassword(USER_ID, "$2a$12$newHash");
            verify(refreshTokenRepository).deleteByUserId(USER_ID);
            verify(passwordResetTokenRepository).delete(resetToken);
        }

        @Test
        void invalidTokenThrowsBadRequest() {
            String rawToken = "invalid-token";
            when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> passwordResetService.resetPassword(rawToken, "newPassword123"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Invalid or expired reset token");
        }

        @Test
        void expiredTokenThrowsBadRequestAndDeletesToken() {
            String rawToken = "expired-token";
            String tokenHash = AuthService.hashToken(rawToken);
            PasswordResetToken expiredToken = PasswordResetToken.builder()
                    .id(UUID.randomUUID())
                    .userId(USER_ID)
                    .tokenHash(tokenHash)
                    .expiresAt(Instant.now().minusSeconds(3600))
                    .build();

            when(passwordResetTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(expiredToken));

            assertThatThrownBy(() -> passwordResetService.resetPassword(rawToken, "newPassword123"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Reset token has expired");

            verify(passwordResetTokenRepository).delete(expiredToken);
        }
    }
}
