package ua.kpi.sc.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import ua.kpi.sc.auth.dto.AuthUserResponse.PartnerRoleDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.kpi.sc.auth.config.JwtProperties;
import ua.kpi.sc.auth.dto.AuthUserResponse;
import ua.kpi.sc.auth.dto.LoginRequest;
import ua.kpi.sc.auth.dto.RegisterRequest;
import ua.kpi.sc.auth.entity.RefreshToken;
import ua.kpi.sc.auth.repository.RefreshTokenRepository;
import ua.kpi.sc.auth.security.JwtTokenProvider;
import ua.kpi.sc.common.audit.AuditAction;
import ua.kpi.sc.common.audit.AuditEntityType;
import ua.kpi.sc.common.audit.AuditEventBuilder;
import ua.kpi.sc.common.audit.AuditPublisher;
import ua.kpi.sc.common.exception.ConflictException;
import ua.kpi.sc.common.notification.NotificationCategory;
import ua.kpi.sc.common.notification.NotificationEventBuilder;
import ua.kpi.sc.common.notification.NotificationPublisher;
import ua.kpi.sc.common.util.InputSanitizer;
import ua.kpi.sc.common.util.PasswordValidator;
import ua.kpi.sc.common.exception.ResourceNotFoundException;
import ua.kpi.sc.common.exception.UnauthorizedException;
import ua.kpi.sc.common.security.TwoFactorQueryPort;
import ua.kpi.sc.common.security.UserDetailsPort;
import ua.kpi.sc.common.security.UserPrincipal;

/**
 * Core authentication service handling register, login, logout, refresh, and user retrieval.
 *
 * @since 0.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserDetailsPort userDetailsPort;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final AuditPublisher auditPublisher;
    private final NotificationPublisher notificationPublisher;
    private final TwoFactorQueryPort twoFactorQueryPort;

    @Transactional
    public AuthResult register(RegisterRequest request) {
        PasswordValidator.validateLength(request.password());
        if (userDetailsPort.existsByEmail(request.email())) {
            throw new ConflictException("Email already registered");
        }

        String passwordHash = passwordEncoder.encode(request.password());
        String firstName = InputSanitizer.stripHtml(request.firstName());
        String lastName = InputSanitizer.stripHtml(request.lastName());
        UserPrincipal principal = userDetailsPort.createUser(
                request.email(), passwordHash, firstName, lastName);

        auditPublisher.publish(AuditEventBuilder.builder()
                .actor(principal)
                .action(AuditAction.REGISTER)
                .entityType(AuditEntityType.AUTH)
                .entityId(principal.getId())
                .entityName(principal.getEmail())
                .sourceModule("auth")
                .build());

        return createAuthResult(principal);
    }

    @Transactional
    public LoginResult login(LoginRequest request) {
        UserPrincipal principal = userDetailsPort.loadByEmail(request.email())
                .orElseThrow(() -> {
                    auditPublisher.publish(AuditEventBuilder.builder()
                            .action(AuditAction.LOGIN_FAILED)
                            .entityType(AuditEntityType.AUTH)
                            .entityName(request.email())
                            .sourceModule("auth")
                            .details("Invalid email")
                            .build());
                    return new UnauthorizedException("Invalid email or password");
                });

        if (principal.getPassword() == null) {
            auditPublisher.publish(AuditEventBuilder.builder()
                    .actorId(principal.getId())
                    .actorEmail(principal.getEmail())
                    .action(AuditAction.LOGIN_FAILED)
                    .entityType(AuditEntityType.AUTH)
                    .entityId(principal.getId())
                    .entityName(principal.getEmail())
                    .sourceModule("auth")
                    .details("OAuth-only account attempted password login")
                    .build());
            throw new UnauthorizedException("This account uses social login. Please sign in with Google.");
        }

        if (!passwordEncoder.matches(request.password(), principal.getPassword())) {
            auditPublisher.publish(AuditEventBuilder.builder()
                    .actorId(principal.getId())
                    .actorEmail(principal.getEmail())
                    .action(AuditAction.LOGIN_FAILED)
                    .entityType(AuditEntityType.AUTH)
                    .entityId(principal.getId())
                    .entityName(principal.getEmail())
                    .sourceModule("auth")
                    .details("Invalid password")
                    .build());
            notificationPublisher.publishToUser(principal.getId(), NotificationEventBuilder.builder()
                    .titleKey("notification.security.login_failed")
                    .bodyKey("notification.security.login_failed.body")
                    .category(NotificationCategory.SECURITY)
                    .sourceModule("auth")
                    .relatedEntityId(principal.getId())
                    .relatedEntityType("AUTH")
                    .build());
            throw new UnauthorizedException("Invalid email or password");
        }

        if (!principal.isEnabled()) {
            auditPublisher.publish(AuditEventBuilder.builder()
                    .actorId(principal.getId())
                    .actorEmail(principal.getEmail())
                    .action(AuditAction.LOGIN_FAILED)
                    .entityType(AuditEntityType.AUTH)
                    .entityId(principal.getId())
                    .entityName(principal.getEmail())
                    .sourceModule("auth")
                    .details("Account disabled")
                    .build());
            throw new UnauthorizedException("Account is disabled");
        }

        boolean twoFactorEnabled = twoFactorQueryPort.isTwoFactorEnabled(principal.getId());

        if (twoFactorEnabled) {
            auditPublisher.publish(AuditEventBuilder.builder()
                    .actor(principal)
                    .action(AuditAction.MFA_CHALLENGE_ISSUED)
                    .entityType(AuditEntityType.AUTH)
                    .entityId(principal.getId())
                    .entityName(principal.getEmail())
                    .sourceModule("auth")
                    .build());
            return new LoginResult(null, principal.getId(), true, true);
        }

        auditPublisher.publish(AuditEventBuilder.builder()
                .actor(principal)
                .action(AuditAction.LOGIN)
                .entityType(AuditEntityType.AUTH)
                .entityId(principal.getId())
                .entityName(principal.getEmail())
                .sourceModule("auth")
                .build());

        notificationPublisher.publishToUser(principal.getId(), NotificationEventBuilder.builder()
                .titleKey("notification.security.login")
                .bodyKey("notification.security.login.body")
                .category(NotificationCategory.SECURITY)
                .sourceModule("auth")
                .relatedEntityId(principal.getId())
                .relatedEntityType("AUTH")
                .build());

        return new LoginResult(createAuthResult(principal), principal.getId(), false, false);
    }

    @Transactional
    public void logout(UUID userId) {
        refreshTokenRepository.deleteByUserId(userId);

        auditPublisher.publish(AuditEventBuilder.builder()
                .actorId(userId)
                .action(AuditAction.LOGOUT)
                .entityType(AuditEntityType.AUTH)
                .entityId(userId)
                .sourceModule("auth")
                .build());
    }

    @Transactional
    public AuthResult refresh(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            throw new UnauthorizedException("Refresh token is required");
        }

        String tokenHash = hashToken(refreshTokenValue);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(storedToken);
            throw new UnauthorizedException("Refresh token expired");
        }

        // Rotate: delete old token
        refreshTokenRepository.delete(storedToken);

        UserPrincipal principal = userDetailsPort.loadById(storedToken.getUserId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        return createAuthResult(principal);
    }

    @Transactional(readOnly = true)
    public AuthUserResponse getMe(UUID userId) {
        UserPrincipal principal = userDetailsPort.loadById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return toResponse(principal);
    }

    AuthResult createAuthResult(UserPrincipal principal) {
        String accessToken = jwtTokenProvider.generateAccessToken(principal);
        String refreshTokenValue = jwtTokenProvider.generateRefreshToken();

        RefreshToken refreshToken = RefreshToken.builder()
                .userId(principal.getId())
                .tokenHash(hashToken(refreshTokenValue))
                .expiresAt(Instant.now().plusMillis(jwtProperties.getRefreshExpiration()))
                .build();
        refreshTokenRepository.save(refreshToken);

        return new AuthResult(accessToken, refreshTokenValue, toResponse(principal));
    }

    /**
     * Creates an AuthResult by loading user by ID. Used after MFA verification.
     */
    @Transactional
    public AuthResult createAuthResultById(UUID userId) {
        UserPrincipal principal = userDetailsPort.loadById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        auditPublisher.publish(AuditEventBuilder.builder()
                .actor(principal)
                .action(AuditAction.LOGIN)
                .entityType(AuditEntityType.AUTH)
                .entityId(principal.getId())
                .entityName(principal.getEmail())
                .sourceModule("auth")
                .details("Login completed after MFA verification")
                .build());

        notificationPublisher.publishToUser(principal.getId(), NotificationEventBuilder.builder()
                .titleKey("notification.security.login")
                .bodyKey("notification.security.login.body")
                .category(NotificationCategory.SECURITY)
                .sourceModule("auth")
                .relatedEntityId(principal.getId())
                .relatedEntityType("AUTH")
                .build());

        return createAuthResult(principal);
    }

    private AuthUserResponse toResponse(UserPrincipal principal) {
        var partnerRoles = principal.getPartnerRoles().entrySet().stream()
                .map(e -> new PartnerRoleDto(e.getKey(), e.getValue().getValue()))
                .toList();

        return new AuthUserResponse(
                principal.getId(),
                principal.getEmail(),
                principal.getFirstName(),
                principal.getLastName(),
                principal.getTier().getLevel(),
                partnerRoles,
                principal.isTwoFactorEnabled()
        );
    }

    public static String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * Carries the result of a successful authentication operation.
     */
    public record AuthResult(String accessToken, String refreshToken, AuthUserResponse user) {
    }

    /**
     * Carries the result of a login attempt, including 2FA state.
     *
     * @param authResult         the auth tokens+user (null when twoFactorRequired is true)
     * @param userId             the authenticated user's ID
     * @param twoFactorRequired  whether 2FA verification is needed to complete login
     * @param twoFactorEnabled   whether the user has 2FA enabled
     */
    public record LoginResult(AuthResult authResult, UUID userId,
                              boolean twoFactorRequired, boolean twoFactorEnabled) {
    }
}
