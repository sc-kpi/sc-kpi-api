package ua.kpi.sc.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.kpi.sc.auth.config.JwtProperties;
import ua.kpi.sc.auth.dto.AuthUserResponse;
import ua.kpi.sc.auth.dto.LoginRequest;
import ua.kpi.sc.auth.dto.RegisterRequest;
import ua.kpi.sc.auth.service.AuthService;
import ua.kpi.sc.auth.util.CookieUtil;
import ua.kpi.sc.common.security.SecurityConstants;
import ua.kpi.sc.common.security.UserPrincipal;

/**
 * REST controller for authentication operations (login, registration, token refresh).
 *
 * @since 0.1.0
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Authentication endpoints")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<AuthUserResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthService.AuthResult result = authService.register(request);
        HttpHeaders headers = createTokenHeaders(result);
        return ResponseEntity.status(HttpStatus.CREATED).headers(headers).body(result.user());
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate with email and password")
    public ResponseEntity<AuthUserResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.AuthResult result = authService.login(request);
        HttpHeaders headers = createTokenHeaders(result);
        return ResponseEntity.ok().headers(headers).body(result.user());
    }

    @PostMapping("/logout")
    @Operation(summary = "Log out and clear tokens")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UserPrincipal principal) {
        authService.logout(principal.getId());
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE,
                CookieUtil.createDeleteCookie(SecurityConstants.ACCESS_TOKEN_COOKIE).toString());
        headers.add(HttpHeaders.SET_COOKIE,
                CookieUtil.createDeleteCookie(SecurityConstants.REFRESH_TOKEN_COOKIE).toString());
        return ResponseEntity.ok().headers(headers).build();
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token cookie")
    public ResponseEntity<AuthUserResponse> refresh(HttpServletRequest request) {
        String refreshTokenValue = extractRefreshTokenFromCookie(request);
        AuthService.AuthResult result = authService.refresh(refreshTokenValue);
        HttpHeaders headers = createTokenHeaders(result);
        return ResponseEntity.ok().headers(headers).body(result.user());
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user info")
    public ResponseEntity<AuthUserResponse> getMe(@AuthenticationPrincipal UserPrincipal principal) {
        AuthUserResponse response = authService.getMe(principal.getId());
        return ResponseEntity.ok(response);
    }

    private HttpHeaders createTokenHeaders(AuthService.AuthResult result) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE,
                CookieUtil.createAccessTokenCookie(
                        result.accessToken(), jwtProperties.getAccessExpiration()).toString());
        headers.add(HttpHeaders.SET_COOKIE,
                CookieUtil.createRefreshTokenCookie(
                        result.refreshToken(), jwtProperties.getRefreshExpiration()).toString());
        return headers;
    }

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (SecurityConstants.REFRESH_TOKEN_COOKIE.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
