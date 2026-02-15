package ua.kpi.sc.auth.controller;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ua.kpi.sc.auth.config.CookieProperties;
import ua.kpi.sc.auth.config.JwtProperties;
import ua.kpi.sc.auth.config.OAuthProperties;
import ua.kpi.sc.auth.service.AuthService;
import ua.kpi.sc.auth.service.OAuthService;
import ua.kpi.sc.auth.util.CookieUtil;
import ua.kpi.sc.common.featureflag.FeatureFlag;

/**
 * REST controller for OAuth 2.0 authentication flows.
 *
 * @since 0.1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth/oauth2")
@Tag(name = "OAuth", description = "OAuth 2.0 authentication endpoints")
@RequiredArgsConstructor
@FeatureFlag("auth.oauth2.google")
public class OAuthController {

    private static final String OAUTH_STATE_COOKIE = "oauth_state";
    private static final int STATE_COOKIE_MAX_AGE = 600; // 10 minutes

    private final OAuthService oAuthService;
    private final OAuthProperties oAuthProperties;
    private final JwtProperties jwtProperties;
    private final CookieProperties cookieProperties;

    @GetMapping("/google")
    @Operation(summary = "Initiate Google OAuth 2.0 login")
    public ResponseEntity<Void> googleLogin(HttpServletResponse response) {
        String state = UUID.randomUUID().toString();

        // Store state in HTTP-only cookie for CSRF protection
        response.addHeader(HttpHeaders.SET_COOKIE,
                CookieUtil.buildCookie(OAUTH_STATE_COOKIE, state, STATE_COOKIE_MAX_AGE,
                        "/api/v1/auth/oauth2", cookieProperties.isSecure()).toString());

        String authorizationUrl = oAuthService.buildGoogleAuthorizationUrl(state);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, authorizationUrl)
                .build();
    }

    @GetMapping("/callback/google")
    @Operation(summary = "Google OAuth 2.0 callback")
    public ResponseEntity<Void> googleCallback(
            @RequestParam String code,
            @RequestParam String state,
            HttpServletRequest request,
            HttpServletResponse response) {
        // Validate state against cookie
        String storedState = extractCookieValue(request, OAUTH_STATE_COOKIE);
        if (storedState == null || !storedState.equals(state)) {
            log.warn("OAuth state mismatch — possible CSRF attack");
            return redirectToFrontendWithError("invalid_state");
        }

        try {
            AuthService.AuthResult result = oAuthService.processGoogleLogin(code);

            // Set auth cookies
            boolean secure = cookieProperties.isSecure();
            response.addHeader(HttpHeaders.SET_COOKIE,
                    CookieUtil.createAccessTokenCookie(
                            result.accessToken(), jwtProperties.getAccessExpiration(), secure).toString());
            response.addHeader(HttpHeaders.SET_COOKIE,
                    CookieUtil.createRefreshTokenCookie(
                            result.refreshToken(), jwtProperties.getRefreshExpiration(), secure).toString());

            // Delete state cookie
            response.addHeader(HttpHeaders.SET_COOKIE,
                    CookieUtil.createDeleteCookie(OAUTH_STATE_COOKIE, "/api/v1/auth/oauth2", secure)
                            .toString());

            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, oAuthProperties.getFrontendUrl())
                    .build();
        } catch (Exception e) {
            log.error("OAuth login failed", e);
            return redirectToFrontendWithError("oauth_failed");
        }
    }

    private ResponseEntity<Void> redirectToFrontendWithError(String error) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION,
                        oAuthProperties.getFrontendUrl() + "/login?error=" + error)
                .build();
    }

    private String extractCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
