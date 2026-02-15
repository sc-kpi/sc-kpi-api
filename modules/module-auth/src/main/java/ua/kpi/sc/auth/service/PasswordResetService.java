package ua.kpi.sc.auth.service;

import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.kpi.sc.auth.config.PasswordResetProperties;
import ua.kpi.sc.auth.entity.PasswordResetToken;
import ua.kpi.sc.auth.repository.PasswordResetTokenRepository;
import ua.kpi.sc.auth.repository.RefreshTokenRepository;
import ua.kpi.sc.common.audit.AuditAction;
import ua.kpi.sc.common.audit.AuditEntityType;
import ua.kpi.sc.common.audit.AuditEventBuilder;
import ua.kpi.sc.common.audit.AuditPublisher;
import ua.kpi.sc.common.exception.BadRequestException;
import ua.kpi.sc.common.util.PasswordValidator;
import ua.kpi.sc.common.security.UserDetailsPort;
import ua.kpi.sc.common.security.UserPrincipal;

/**
 * Handles the password-reset flow: token generation and password update.
 *
 * @since 0.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserDetailsPort userDetailsPort;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetProperties properties;
    private final EmailService emailService;
    private final AuditPublisher auditPublisher;

    /**
     * Initiates a password reset for the given email.
     * Always returns silently to prevent email enumeration.
     */
    @Transactional
    public void requestPasswordReset(String email) {
        var optionalPrincipal = userDetailsPort.loadByEmail(email);
        if (optionalPrincipal.isEmpty()) {
            log.debug("Password reset requested for non-existent email: {}", email);
            return;
        }

        UserPrincipal principal = optionalPrincipal.get();

        // OAuth-only users have no password — silently ignore
        if (principal.getPassword() == null) {
            log.debug("Password reset requested for OAuth-only account: {}", email);
            return;
        }

        // Delete any existing tokens for this user
        passwordResetTokenRepository.deleteByUserId(principal.getId());

        // Generate token and store hash
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = AuthService.hashToken(rawToken);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .userId(principal.getId())
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plusMillis(properties.getTokenExpiration()))
                .build();
        passwordResetTokenRepository.save(resetToken);

        // Send email asynchronously
        String resetLink = properties.getBaseUrl() + "/reset-password?token=" + rawToken;
        emailService.sendPasswordResetEmail(email, resetLink);

        auditPublisher.publish(AuditEventBuilder.builder()
                .actorId(principal.getId())
                .actorEmail(principal.getEmail())
                .action(AuditAction.PASSWORD_RESET_REQUESTED)
                .entityType(AuditEntityType.AUTH)
                .entityId(principal.getId())
                .entityName(principal.getEmail())
                .sourceModule("auth")
                .build());
    }

    /**
     * Validates the token and sets the new password.
     * Invalidates all refresh tokens for the user.
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordValidator.validateLength(newPassword);
        String tokenHash = AuthService.hashToken(token);

        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            passwordResetTokenRepository.delete(resetToken);
            throw new BadRequestException("Reset token has expired");
        }

        // Update password
        String passwordHash = passwordEncoder.encode(newPassword);
        userDetailsPort.updatePassword(resetToken.getUserId(), passwordHash);

        // Invalidate all refresh tokens
        refreshTokenRepository.deleteByUserId(resetToken.getUserId());

        // Delete the used reset token
        passwordResetTokenRepository.delete(resetToken);

        auditPublisher.publish(AuditEventBuilder.builder()
                .actorId(resetToken.getUserId())
                .action(AuditAction.PASSWORD_RESET_COMPLETED)
                .entityType(AuditEntityType.AUTH)
                .entityId(resetToken.getUserId())
                .sourceModule("auth")
                .build());

        log.info("Password reset completed for user {}", resetToken.getUserId());
    }
}
