package ua.kpi.sc.auth.service;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import ua.kpi.sc.auth.config.OAuthProperties;
import ua.kpi.sc.auth.dto.AuthUserResponse;
import ua.kpi.sc.auth.dto.GoogleTokenResponse;
import ua.kpi.sc.auth.dto.GoogleUserInfo;
import ua.kpi.sc.auth.entity.OAuthAccount;
import ua.kpi.sc.auth.repository.OAuthAccountRepository;
import ua.kpi.sc.common.audit.AuditPublisher;
import ua.kpi.sc.common.exception.BadRequestException;
import ua.kpi.sc.common.exception.UnauthorizedException;
import ua.kpi.sc.common.security.CapabilityTier;
import ua.kpi.sc.common.security.UserDetailsPort;
import ua.kpi.sc.common.security.UserPrincipal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuthServiceTest {

    @Mock
    private OAuthProperties oAuthProperties;
    @Mock
    private OAuthAccountRepository oAuthAccountRepository;
    @Mock
    private UserDetailsPort userDetailsPort;
    @Mock
    private AuthService authService;
    @Mock
    private RestClient oAuthRestClient;
    @Mock
    private AuditPublisher auditPublisher;

    @InjectMocks
    @Spy
    private OAuthService oAuthService;

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

    private UserPrincipal disabledPrincipal() {
        return UserPrincipal.builder()
                .id(USER_ID)
                .email("disabled@kpi.ua")
                .password(null)
                .firstName("Disabled")
                .lastName("User")
                .tier(CapabilityTier.BASIC)
                .active(false)
                .build();
    }

    private GoogleTokenResponse testTokenResponse() {
        return new GoogleTokenResponse("google-access-token", "id-token", "Bearer", 3600, "openid email profile");
    }

    private GoogleUserInfo verifiedUserInfo() {
        return new GoogleUserInfo("google-sub-123", "test@kpi.ua", true, "Test", "User", null);
    }

    private GoogleUserInfo unverifiedUserInfo() {
        return new GoogleUserInfo("google-sub-456", "unverified@kpi.ua", false, "Unverified", "User", null);
    }

    private AuthService.AuthResult testAuthResult() {
        return new AuthService.AuthResult("access-token", "refresh-token",
                new AuthUserResponse(USER_ID, "test@kpi.ua", "Test", "User", 1, java.util.List.of()));
    }

    @Nested
    class BuildGoogleAuthorizationUrlTests {

        @Test
        void returnsValidUrlWithCorrectParams() {
            when(oAuthProperties.getClientId()).thenReturn("test-client-id");
            when(oAuthProperties.getRedirectUri()).thenReturn("http://localhost:8080/api/v1/auth/oauth2/callback/google");

            String url = oAuthService.buildGoogleAuthorizationUrl("random-state");

            assertThat(url).startsWith("https://accounts.google.com/o/oauth2/v2/auth");
            assertThat(url).contains("client_id=test-client-id");
            assertThat(url).contains("redirect_uri=");
            assertThat(url).contains("response_type=code");
            assertThat(url).contains("scope=");
            assertThat(url).contains("state=random-state");
            assertThat(url).contains("access_type=offline");
            assertThat(url).contains("prompt=consent");
        }
    }

    @Nested
    class ProcessGoogleLoginTests {

        @Test
        void existingOAuthAccountReturnsAuthResult() {
            doReturn(testTokenResponse()).when(oAuthService).exchangeCodeForTokens("auth-code");
            doReturn(verifiedUserInfo()).when(oAuthService).fetchUserInfo("google-access-token");

            OAuthAccount existingAccount = OAuthAccount.builder()
                    .id(UUID.randomUUID())
                    .userId(USER_ID)
                    .provider("google")
                    .providerUserId("google-sub-123")
                    .email("test@kpi.ua")
                    .build();
            when(oAuthAccountRepository.findByProviderAndProviderUserId("google", "google-sub-123"))
                    .thenReturn(Optional.of(existingAccount));
            when(userDetailsPort.loadById(USER_ID)).thenReturn(Optional.of(testPrincipal()));
            when(authService.createAuthResult(any())).thenReturn(testAuthResult());

            AuthService.AuthResult result = oAuthService.processGoogleLogin("auth-code");

            assertThat(result.accessToken()).isEqualTo("access-token");
            assertThat(result.refreshToken()).isEqualTo("refresh-token");
        }

        @Test
        void existingUserByEmailLinksOAuthAndReturnsAuthResult() {
            doReturn(testTokenResponse()).when(oAuthService).exchangeCodeForTokens("auth-code");
            doReturn(verifiedUserInfo()).when(oAuthService).fetchUserInfo("google-access-token");

            when(oAuthAccountRepository.findByProviderAndProviderUserId("google", "google-sub-123"))
                    .thenReturn(Optional.empty());
            when(userDetailsPort.loadByEmail("test@kpi.ua")).thenReturn(Optional.of(testPrincipal()));
            when(oAuthAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(authService.createAuthResult(any())).thenReturn(testAuthResult());

            AuthService.AuthResult result = oAuthService.processGoogleLogin("auth-code");

            assertThat(result.accessToken()).isEqualTo("access-token");
            verify(oAuthAccountRepository).save(any(OAuthAccount.class));
        }

        @Test
        void newUserCreatesUserAndLinksOAuth() {
            doReturn(testTokenResponse()).when(oAuthService).exchangeCodeForTokens("auth-code");
            doReturn(verifiedUserInfo()).when(oAuthService).fetchUserInfo("google-access-token");

            when(oAuthAccountRepository.findByProviderAndProviderUserId("google", "google-sub-123"))
                    .thenReturn(Optional.empty());
            when(userDetailsPort.loadByEmail("test@kpi.ua")).thenReturn(Optional.empty());
            when(userDetailsPort.createUser("test@kpi.ua", null, "Test", "User")).thenReturn(testPrincipal());
            when(oAuthAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(authService.createAuthResult(any())).thenReturn(testAuthResult());

            AuthService.AuthResult result = oAuthService.processGoogleLogin("auth-code");

            assertThat(result.accessToken()).isEqualTo("access-token");
            verify(userDetailsPort).createUser("test@kpi.ua", null, "Test", "User");
            verify(oAuthAccountRepository).save(any(OAuthAccount.class));
        }

        @Test
        void unverifiedEmailThrowsBadRequest() {
            doReturn(testTokenResponse()).when(oAuthService).exchangeCodeForTokens("auth-code");
            doReturn(unverifiedUserInfo()).when(oAuthService).fetchUserInfo("google-access-token");

            assertThatThrownBy(() -> oAuthService.processGoogleLogin("auth-code"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("email is not verified");
        }

        @Test
        void disabledAccountThrowsUnauthorized() {
            doReturn(testTokenResponse()).when(oAuthService).exchangeCodeForTokens("auth-code");
            doReturn(verifiedUserInfo()).when(oAuthService).fetchUserInfo("google-access-token");

            OAuthAccount existingAccount = OAuthAccount.builder()
                    .id(UUID.randomUUID())
                    .userId(USER_ID)
                    .provider("google")
                    .providerUserId("google-sub-123")
                    .email("test@kpi.ua")
                    .build();
            when(oAuthAccountRepository.findByProviderAndProviderUserId("google", "google-sub-123"))
                    .thenReturn(Optional.of(existingAccount));
            when(userDetailsPort.loadById(USER_ID)).thenReturn(Optional.of(disabledPrincipal()));

            assertThatThrownBy(() -> oAuthService.processGoogleLogin("auth-code"))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Account is disabled");
        }
    }
}
