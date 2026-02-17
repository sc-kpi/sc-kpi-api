package ua.kpi.sc.auth.controller;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.kpi.sc.auth.config.CookieProperties;
import ua.kpi.sc.auth.config.JwtProperties;
import ua.kpi.sc.auth.dto.LoginResponse;
import ua.kpi.sc.auth.dto.RecoveryCodesResponse;
import ua.kpi.sc.auth.dto.TotpDisableRequest;
import ua.kpi.sc.auth.dto.TotpSetupResponse;
import ua.kpi.sc.auth.dto.TotpStatusResponse;
import ua.kpi.sc.auth.dto.TotpVerifyRequest;
import ua.kpi.sc.auth.service.AuthService;
import ua.kpi.sc.auth.service.MfaTokenService;
import ua.kpi.sc.auth.service.TotpService;
import ua.kpi.sc.auth.util.CookieUtil;
import ua.kpi.sc.common.audit.AuditAction;
import ua.kpi.sc.common.audit.AuditEntityType;
import ua.kpi.sc.common.audit.AuditEventBuilder;
import ua.kpi.sc.common.audit.AuditPublisher;
import ua.kpi.sc.common.exception.UnauthorizedException;
import ua.kpi.sc.common.featureflag.FeatureFlag;
import ua.kpi.sc.common.security.SecurityConstants;
import ua.kpi.sc.common.security.UserPrincipal;

/**
 * REST controller for TOTP two-factor authentication operations.
 *
 * @since 0.6.0
 */
@RestController
@RequestMapping("/api/v1/auth/2fa")
@Tag(name = "Two-Factor Authentication", description = "TOTP 2FA setup, verification, and management")
@RequiredArgsConstructor
public class TotpController {

    private final TotpService totpService;
    private final AuthService authService;
    private final MfaTokenService mfaTokenService;
    private final AuditPublisher auditPublisher;
    private final JwtProperties jwtProperties;
    private final CookieProperties cookieProperties;

    @PostMapping("/setup")
    @Operation(summary = "Start TOTP 2FA setup")
    @FeatureFlag("auth.mfa.totp")
    public ResponseEntity<TotpSetupResponse> setup(@AuthenticationPrincipal UserPrincipal principal) {
        TotpSetupResponse response = totpService.setupTotp(principal.getId(), principal.getEmail());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-setup")
    @Operation(summary = "Complete TOTP setup by verifying a code")
    @FeatureFlag("auth.mfa.totp")
    public ResponseEntity<RecoveryCodesResponse> verifySetup(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TotpVerifyRequest request) {
        RecoveryCodesResponse response = totpService.verifyAndEnable(principal.getId(), request.code());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/disable")
    @Operation(summary = "Disable TOTP 2FA")
    @FeatureFlag("auth.mfa.totp")
    public ResponseEntity<Void> disable(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TotpDisableRequest request) {
        totpService.disableTotp(principal.getId(), request.password(), request.code());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status")
    @Operation(summary = "Get current 2FA status")
    @FeatureFlag("auth.mfa.totp")
    public ResponseEntity<TotpStatusResponse> status(@AuthenticationPrincipal UserPrincipal principal) {
        TotpStatusResponse response = totpService.getStatus(principal.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/recovery-codes/regenerate")
    @Operation(summary = "Regenerate recovery codes")
    @FeatureFlag("auth.mfa.totp")
    public ResponseEntity<RecoveryCodesResponse> regenerateRecoveryCodes(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TotpVerifyRequest request) {
        RecoveryCodesResponse response = totpService.regenerateRecoveryCodes(principal.getId(), request.code());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-login")
    @Operation(summary = "Complete MFA login by verifying TOTP code or recovery code")
    public ResponseEntity<LoginResponse> verifyLogin(
            HttpServletRequest request,
            @Valid @RequestBody TotpVerifyRequest verifyRequest) {
        String mfaToken = extractMfaTokenFromCookie(request);
        if (mfaToken == null) {
            throw new UnauthorizedException("MFA token is required");
        }

        UUID userId = mfaTokenService.validateAndExtractUserId(mfaToken);

        String code = verifyRequest.code();
        boolean verified;

        // Try TOTP code first, then recovery code
        if (code.contains("-") || code.length() > 6) {
            verified = totpService.verifyRecoveryCode(userId, code);
        } else {
            verified = totpService.verifyCode(userId, code);
        }

        if (!verified) {
            auditPublisher.publish(AuditEventBuilder.builder()
                    .actorId(userId)
                    .action(AuditAction.MFA_VERIFICATION_FAILED)
                    .entityType(AuditEntityType.AUTH)
                    .entityId(userId)
                    .sourceModule("auth")
                    .build());
            throw new UnauthorizedException("Invalid verification code");
        }

        auditPublisher.publish(AuditEventBuilder.builder()
                .actorId(userId)
                .action(AuditAction.MFA_VERIFIED)
                .entityType(AuditEntityType.AUTH)
                .entityId(userId)
                .sourceModule("auth")
                .build());

        AuthService.AuthResult authResult = authService.createAuthResultById(userId);

        HttpHeaders headers = new HttpHeaders();
        boolean secure = cookieProperties.isSecure();
        headers.add(HttpHeaders.SET_COOKIE,
                CookieUtil.createAccessTokenCookie(
                        authResult.accessToken(), jwtProperties.getAccessExpiration(), secure).toString());
        headers.add(HttpHeaders.SET_COOKIE,
                CookieUtil.createRefreshTokenCookie(
                        authResult.refreshToken(), jwtProperties.getRefreshExpiration(), secure).toString());
        // Clear the mfa_token cookie
        headers.add(HttpHeaders.SET_COOKIE,
                CookieUtil.createDeleteCookie(SecurityConstants.MFA_TOKEN_COOKIE,
                        "/api/v1/auth/2fa", secure).toString());

        return ResponseEntity.ok().headers(headers)
                .body(LoginResponse.fromAuthUser(authResult.user(), true));
    }

    private String extractMfaTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (SecurityConstants.MFA_TOKEN_COOKIE.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
