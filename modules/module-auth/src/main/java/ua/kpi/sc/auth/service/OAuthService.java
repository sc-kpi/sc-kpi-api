package ua.kpi.sc.auth.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import ua.kpi.sc.auth.config.OAuthProperties;
import ua.kpi.sc.auth.dto.GoogleTokenResponse;
import ua.kpi.sc.auth.dto.GoogleUserInfo;
import ua.kpi.sc.auth.entity.OAuthAccount;
import ua.kpi.sc.auth.repository.OAuthAccountRepository;
import ua.kpi.sc.common.audit.AuditAction;
import ua.kpi.sc.common.audit.AuditEntityType;
import ua.kpi.sc.common.audit.AuditEventBuilder;
import ua.kpi.sc.common.audit.AuditPublisher;
import ua.kpi.sc.common.exception.BadRequestException;
import ua.kpi.sc.common.exception.UnauthorizedException;
import ua.kpi.sc.common.security.UserDetailsPort;
import ua.kpi.sc.common.security.UserPrincipal;

/**
 * Handles Google OAuth 2.0 authentication flow.
 *
 * @since 0.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthService {

    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";
    private static final String PROVIDER = "google";

    private final OAuthProperties oAuthProperties;
    private final OAuthAccountRepository oAuthAccountRepository;
    private final UserDetailsPort userDetailsPort;
    private final AuthService authService;
    private final RestClient oAuthRestClient;
    private final AuditPublisher auditPublisher;

    /**
     * Builds the Google authorization URL for the consent screen redirect.
     */
    public String buildGoogleAuthorizationUrl(String state) {
        return GOOGLE_AUTH_URL
                + "?client_id=" + encode(oAuthProperties.getClientId())
                + "&redirect_uri=" + encode(oAuthProperties.getRedirectUri())
                + "&response_type=code"
                + "&scope=" + encode("openid email profile")
                + "&state=" + encode(state)
                + "&access_type=offline"
                + "&prompt=consent";
    }

    /**
     * Exchanges the authorization code for tokens and processes user login/registration.
     */
    @Transactional
    public AuthService.AuthResult processGoogleLogin(String code) {
        GoogleTokenResponse tokenResponse = exchangeCodeForTokens(code);
        GoogleUserInfo userInfo = fetchUserInfo(tokenResponse.accessToken());

        if (!userInfo.emailVerified()) {
            throw new BadRequestException("Google account email is not verified");
        }

        UserPrincipal principal = resolveUser(userInfo);

        if (!principal.isEnabled()) {
            throw new UnauthorizedException("Account is disabled");
        }

        return authService.createAuthResult(principal);
    }

    GoogleTokenResponse exchangeCodeForTokens(String code) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", oAuthProperties.getClientId());
        body.add("client_secret", oAuthProperties.getClientSecret());
        body.add("redirect_uri", oAuthProperties.getRedirectUri());
        body.add("grant_type", "authorization_code");

        return oAuthRestClient.post()
                .uri(GOOGLE_TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(GoogleTokenResponse.class);
    }

    GoogleUserInfo fetchUserInfo(String accessToken) {
        return oAuthRestClient.get()
                .uri(GOOGLE_USERINFO_URL)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(GoogleUserInfo.class);
    }

    private UserPrincipal resolveUser(GoogleUserInfo userInfo) {
        // 1. Existing OAuth account → login
        Optional<OAuthAccount> existingOAuth = oAuthAccountRepository
                .findByProviderAndProviderUserId(PROVIDER, userInfo.sub());
        if (existingOAuth.isPresent()) {
            UserPrincipal principal = userDetailsPort.loadById(existingOAuth.get().getUserId())
                    .orElseThrow(() -> new UnauthorizedException("User not found"));
            auditPublisher.publish(AuditEventBuilder.builder()
                    .actor(principal)
                    .action(AuditAction.OAUTH_LOGIN)
                    .entityType(AuditEntityType.AUTH)
                    .entityId(principal.getId())
                    .entityName(principal.getEmail())
                    .sourceModule("auth")
                    .details("Google OAuth login")
                    .build());
            return principal;
        }

        // 2. Existing user by email → link + login
        Optional<UserPrincipal> existingUser = userDetailsPort.loadByEmail(userInfo.email());
        if (existingUser.isPresent()) {
            linkOAuthAccount(existingUser.get().getId(), userInfo);
            auditPublisher.publish(AuditEventBuilder.builder()
                    .actor(existingUser.get())
                    .action(AuditAction.OAUTH_LINKED)
                    .entityType(AuditEntityType.AUTH)
                    .entityId(existingUser.get().getId())
                    .entityName(existingUser.get().getEmail())
                    .sourceModule("auth")
                    .details("Google account linked")
                    .build());
            return existingUser.get();
        }

        // 3. New user → create with null password + link
        String firstName = userInfo.givenName() != null ? userInfo.givenName() : "";
        String lastName = userInfo.familyName() != null ? userInfo.familyName() : "";
        UserPrincipal newUser = userDetailsPort.createUser(userInfo.email(), null, firstName, lastName);
        linkOAuthAccount(newUser.getId(), userInfo);
        auditPublisher.publish(AuditEventBuilder.builder()
                .actor(newUser)
                .action(AuditAction.REGISTER)
                .entityType(AuditEntityType.AUTH)
                .entityId(newUser.getId())
                .entityName(newUser.getEmail())
                .sourceModule("auth")
                .details("via Google OAuth")
                .build());
        return newUser;
    }

    private void linkOAuthAccount(UUID userId, GoogleUserInfo userInfo) {
        OAuthAccount account = OAuthAccount.builder()
                .userId(userId)
                .provider(PROVIDER)
                .providerUserId(userInfo.sub())
                .email(userInfo.email())
                .build();
        oAuthAccountRepository.save(account);
        log.info("Linked Google account {} to user {}", userInfo.sub(), userId);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
