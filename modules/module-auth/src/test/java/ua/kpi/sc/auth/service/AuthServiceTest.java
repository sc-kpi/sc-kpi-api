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
import ua.kpi.sc.auth.config.JwtProperties;
import ua.kpi.sc.auth.entity.RefreshToken;
import ua.kpi.sc.auth.repository.RefreshTokenRepository;
import ua.kpi.sc.auth.security.JwtTokenProvider;
import ua.kpi.sc.common.audit.AuditPublisher;
import ua.kpi.sc.common.notification.NotificationPublisher;
import ua.kpi.sc.common.exception.ConflictException;
import ua.kpi.sc.common.exception.ResourceNotFoundException;
import ua.kpi.sc.common.exception.UnauthorizedException;
import ua.kpi.sc.common.security.CapabilityTier;
import ua.kpi.sc.common.security.TwoFactorQueryPort;
import ua.kpi.sc.common.security.UserDetailsPort;
import ua.kpi.sc.common.security.UserPrincipal;
import ua.kpi.sc.auth.dto.LoginRequest;
import ua.kpi.sc.auth.dto.RegisterRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserDetailsPort userDetailsPort;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private JwtProperties jwtProperties;
    @Mock
    private AuditPublisher auditPublisher;
    @Mock
    private NotificationPublisher notificationPublisher;
    @Mock
    private TwoFactorQueryPort twoFactorQueryPort;

    @InjectMocks
    private AuthService authService;

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

    @Nested
    class RegisterTests {

        @Test
        void registerSuccessReturnsAuthResult() {
            RegisterRequest request = new RegisterRequest("new@kpi.ua", "password123", "New", "User");
            when(userDetailsPort.existsByEmail("new@kpi.ua")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("$2a$12$encoded");
            when(userDetailsPort.createUser("new@kpi.ua", "$2a$12$encoded", "New", "User"))
                    .thenReturn(testPrincipal());
            when(jwtTokenProvider.generateAccessToken(any())).thenReturn("access-token");
            when(jwtTokenProvider.generateRefreshToken()).thenReturn("refresh-token");
            when(jwtProperties.getRefreshExpiration()).thenReturn(2592000000L);
            when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AuthService.AuthResult result = authService.register(request);

            assertThat(result.accessToken()).isEqualTo("access-token");
            assertThat(result.refreshToken()).isEqualTo("refresh-token");
            assertThat(result.user().email()).isEqualTo("test@kpi.ua");
        }

        @Test
        void registerDuplicateEmailThrowsConflict() {
            RegisterRequest request = new RegisterRequest("dup@kpi.ua", "pass1234", "Dup", "User");
            when(userDetailsPort.existsByEmail("dup@kpi.ua")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Email already registered");
        }
    }

    @Nested
    class LoginTests {

        @Test
        void loginSuccessReturnsLoginResult() {
            LoginRequest request = new LoginRequest("test@kpi.ua", "password123");
            when(userDetailsPort.loadByEmail("test@kpi.ua")).thenReturn(Optional.of(testPrincipal()));
            when(passwordEncoder.matches("password123", "$2a$12$hashed")).thenReturn(true);
            when(twoFactorQueryPort.isTwoFactorEnabled(USER_ID)).thenReturn(false);
            when(jwtTokenProvider.generateAccessToken(any())).thenReturn("access-token");
            when(jwtTokenProvider.generateRefreshToken()).thenReturn("refresh-token");
            when(jwtProperties.getRefreshExpiration()).thenReturn(2592000000L);
            when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AuthService.LoginResult result = authService.login(request);

            assertThat(result.twoFactorRequired()).isFalse();
            assertThat(result.authResult()).isNotNull();
            assertThat(result.authResult().accessToken()).isEqualTo("access-token");
            assertThat(result.authResult().user().email()).isEqualTo("test@kpi.ua");
        }

        @Test
        void loginWithTwoFactorEnabledReturnsMfaChallenge() {
            LoginRequest request = new LoginRequest("test@kpi.ua", "password123");
            when(userDetailsPort.loadByEmail("test@kpi.ua")).thenReturn(Optional.of(testPrincipal()));
            when(passwordEncoder.matches("password123", "$2a$12$hashed")).thenReturn(true);
            when(twoFactorQueryPort.isTwoFactorEnabled(USER_ID)).thenReturn(true);

            AuthService.LoginResult result = authService.login(request);

            assertThat(result.twoFactorRequired()).isTrue();
            assertThat(result.authResult()).isNull();
            assertThat(result.userId()).isEqualTo(USER_ID);
        }

        @Test
        void loginWrongEmailThrowsUnauthorized() {
            LoginRequest request = new LoginRequest("wrong@kpi.ua", "password123");
            when(userDetailsPort.loadByEmail("wrong@kpi.ua")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Invalid email or password");
        }

        @Test
        void loginWrongPasswordThrowsUnauthorized() {
            LoginRequest request = new LoginRequest("test@kpi.ua", "wrong");
            when(userDetailsPort.loadByEmail("test@kpi.ua")).thenReturn(Optional.of(testPrincipal()));
            when(passwordEncoder.matches("wrong", "$2a$12$hashed")).thenReturn(false);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Invalid email or password");
        }

        @Test
        void loginOAuthOnlyUserNullPasswordThrowsUnauthorized() {
            LoginRequest request = new LoginRequest("oauth@kpi.ua", "password123");
            UserPrincipal oauthPrincipal = UserPrincipal.builder()
                    .id(USER_ID).email("oauth@kpi.ua").password(null)
                    .firstName("OAuth").lastName("User")
                    .tier(CapabilityTier.BASIC).active(true).build();
            when(userDetailsPort.loadByEmail("oauth@kpi.ua")).thenReturn(Optional.of(oauthPrincipal));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("social login");
        }

        @Test
        void loginDisabledAccountThrowsUnauthorized() {
            LoginRequest request = new LoginRequest("test@kpi.ua", "password123");
            UserPrincipal disabled = UserPrincipal.builder()
                    .id(USER_ID).email("test@kpi.ua").password("$2a$12$hashed")
                    .firstName("Test").lastName("User")
                    .tier(CapabilityTier.BASIC).active(false).build();
            when(userDetailsPort.loadByEmail("test@kpi.ua")).thenReturn(Optional.of(disabled));
            when(passwordEncoder.matches("password123", "$2a$12$hashed")).thenReturn(true);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Account is disabled");
        }
    }

    @Nested
    class LogoutTests {

        @Test
        void logoutDeletesRefreshTokens() {
            authService.logout(USER_ID);
            verify(refreshTokenRepository).deleteByUserId(USER_ID);
        }
    }

    @Nested
    class RefreshTests {

        @Test
        void refreshValidTokenRotatesAndReturns() {
            String tokenValue = "valid-refresh-token";
            String tokenHash = AuthService.hashToken(tokenValue);
            RefreshToken stored = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .userId(USER_ID)
                    .tokenHash(tokenHash)
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();

            when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(stored));
            when(userDetailsPort.loadById(USER_ID)).thenReturn(Optional.of(testPrincipal()));
            when(jwtTokenProvider.generateAccessToken(any())).thenReturn("new-access-token");
            when(jwtTokenProvider.generateRefreshToken()).thenReturn("new-refresh-token");
            when(jwtProperties.getRefreshExpiration()).thenReturn(2592000000L);
            when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AuthService.AuthResult result = authService.refresh(tokenValue);

            assertThat(result.accessToken()).isEqualTo("new-access-token");
            verify(refreshTokenRepository).delete(stored);
        }

        @Test
        void refreshInvalidTokenThrowsUnauthorized() {
            String tokenValue = "invalid";
            when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refresh(tokenValue))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Invalid refresh token");
        }

        @Test
        void refreshExpiredTokenThrowsUnauthorized() {
            String tokenValue = "expired-token";
            String tokenHash = AuthService.hashToken(tokenValue);
            RefreshToken expired = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .userId(USER_ID)
                    .tokenHash(tokenHash)
                    .expiresAt(Instant.now().minusSeconds(3600))
                    .build();

            when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(expired));

            assertThatThrownBy(() -> authService.refresh(tokenValue))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Refresh token expired");
        }

        @Test
        void refreshNullTokenThrowsUnauthorized() {
            assertThatThrownBy(() -> authService.refresh(null))
                    .isInstanceOf(UnauthorizedException.class);
        }
    }

    @Nested
    class GetMeTests {

        @Test
        void getMeReturnsUserResponse() {
            when(userDetailsPort.loadById(USER_ID)).thenReturn(Optional.of(testPrincipal()));

            var response = authService.getMe(USER_ID);

            assertThat(response.id()).isEqualTo(USER_ID);
            assertThat(response.email()).isEqualTo("test@kpi.ua");
            assertThat(response.firstName()).isEqualTo("Test");
            assertThat(response.lastName()).isEqualTo("User");
            assertThat(response.capabilityTier()).isEqualTo(1);
        }

        @Test
        void getMeNonExistentThrowsNotFound() {
            when(userDetailsPort.loadById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.getMe(USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
