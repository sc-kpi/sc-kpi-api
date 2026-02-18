package ua.kpi.sc.auth.service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.bastiaanjansen.otp.HMACAlgorithm;
import com.bastiaanjansen.otp.SecretGenerator;
import com.bastiaanjansen.otp.TOTPGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.kpi.sc.auth.config.MfaProperties;
import ua.kpi.sc.auth.dto.RecoveryCodesResponse;
import ua.kpi.sc.auth.dto.TotpSetupResponse;
import ua.kpi.sc.auth.dto.TotpStatusResponse;
import ua.kpi.sc.auth.entity.TotpRecoveryCode;
import ua.kpi.sc.auth.entity.TotpSecret;
import ua.kpi.sc.auth.repository.TotpRecoveryCodeRepository;
import ua.kpi.sc.auth.repository.TotpSecretRepository;
import ua.kpi.sc.common.audit.AuditAction;
import ua.kpi.sc.common.audit.AuditEntityType;
import ua.kpi.sc.common.audit.AuditEventBuilder;
import ua.kpi.sc.common.audit.AuditPublisher;
import ua.kpi.sc.common.exception.BadRequestException;
import ua.kpi.sc.common.exception.ResourceNotFoundException;
import ua.kpi.sc.common.exception.UnauthorizedException;
import ua.kpi.sc.common.notification.NotificationCategory;
import ua.kpi.sc.common.notification.NotificationEventBuilder;
import ua.kpi.sc.common.notification.NotificationPublisher;
import ua.kpi.sc.common.security.UserDetailsPort;
import ua.kpi.sc.common.security.UserPrincipal;

/**
 * Core TOTP two-factor authentication service.
 * Handles setup, verification, recovery codes, and lifecycle management.
 *
 * @since 0.6.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TotpService {

    private final TotpSecretRepository totpSecretRepository;
    private final TotpRecoveryCodeRepository totpRecoveryCodeRepository;
    private final TotpEncryptionService encryptionService;
    private final QrCodeService qrCodeService;
    private final MfaProperties mfaProperties;
    private final AuditPublisher auditPublisher;
    private final NotificationPublisher notificationPublisher;
    private final UserDetailsPort userDetailsPort;
    private final PasswordEncoder passwordEncoder;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String RECOVERY_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    /**
     * Initiates TOTP setup for a user. Generates a secret, stores it encrypted,
     * and returns a QR code data URI + manual entry key. Does NOT enable 2FA yet.
     */
    @Transactional
    public TotpSetupResponse setupTotp(UUID userId, String userEmail) {
        // Remove any existing non-enabled setup
        totpSecretRepository.findByUserId(userId).ifPresent(existing -> {
            if (existing.isEnabled()) {
                throw new BadRequestException("Two-factor authentication is already enabled");
            }
            totpSecretRepository.delete(existing);
            totpSecretRepository.flush();
        });

        // SecretGenerator.generate() returns base32-encoded bytes (not raw bytes),
        // so we convert to String directly — do NOT re-encode with base32.
        byte[] secret = SecretGenerator.generate();
        String base32Secret = new String(secret, StandardCharsets.US_ASCII);
        String encryptedSecret = encryptionService.encrypt(base32Secret);

        MfaProperties.TotpConfig totpConfig = mfaProperties.getTotp();
        TotpSecret totpSecret = TotpSecret.builder()
                .userId(userId)
                .encryptedSecret(encryptedSecret)
                .algorithm(totpConfig.getAlgorithm())
                .digits(totpConfig.getDigits())
                .period(totpConfig.getPeriod())
                .enabled(false)
                .build();
        totpSecretRepository.save(totpSecret);

        String otpauthUri = buildOtpauthUri(base32Secret, userEmail, totpConfig);
        String qrCodeDataUri = qrCodeService.generateDataUri(otpauthUri);

        return new TotpSetupResponse(qrCodeDataUri, base32Secret);
    }

    /**
     * Verifies a TOTP code against the stored (not yet enabled) secret,
     * enables 2FA, and generates recovery codes.
     */
    @Transactional
    public RecoveryCodesResponse verifyAndEnable(UUID userId, String code) {
        TotpSecret totpSecret = totpSecretRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("TOTP setup not started"));

        if (totpSecret.isEnabled()) {
            throw new BadRequestException("Two-factor authentication is already enabled");
        }

        String base32Secret = encryptionService.decrypt(totpSecret.getEncryptedSecret());
        if (!verifyTotpCode(base32Secret, code, totpSecret)) {
            throw new BadRequestException("Invalid verification code");
        }

        totpSecret.setEnabled(true);
        totpSecret.setUpdatedAt(Instant.now());
        totpSecretRepository.save(totpSecret);

        List<String> plainCodes = generateAndStoreRecoveryCodes(userId);

        auditPublisher.publish(AuditEventBuilder.builder()
                .actorId(userId)
                .action(AuditAction.MFA_ENABLED)
                .entityType(AuditEntityType.AUTH)
                .entityId(userId)
                .sourceModule("auth")
                .build());

        notificationPublisher.publishToUser(userId, NotificationEventBuilder.builder()
                .titleKey("notification.security.mfa_enabled")
                .bodyKey("notification.security.mfa_enabled.body")
                .category(NotificationCategory.SECURITY)
                .sourceModule("auth")
                .relatedEntityId(userId)
                .relatedEntityType("AUTH")
                .build());

        return new RecoveryCodesResponse(plainCodes);
    }

    /**
     * Verifies a TOTP code for login purposes (2FA is already enabled).
     */
    @Transactional(readOnly = true)
    public boolean verifyCode(UUID userId, String code) {
        TotpSecret totpSecret = totpSecretRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Two-factor authentication is not set up"));

        if (!totpSecret.isEnabled()) {
            throw new BadRequestException("Two-factor authentication is not enabled");
        }

        String base32Secret = encryptionService.decrypt(totpSecret.getEncryptedSecret());
        return verifyTotpCode(base32Secret, code, totpSecret);
    }

    /**
     * Verifies a recovery code and marks it as used.
     */
    @Transactional
    public boolean verifyRecoveryCode(UUID userId, String code) {
        String codeHash = AuthService.hashToken(code.replace("-", "").toUpperCase());
        List<TotpRecoveryCode> unusedCodes = totpRecoveryCodeRepository.findByUserIdAndUsedFalse(userId);

        for (TotpRecoveryCode recoveryCode : unusedCodes) {
            if (recoveryCode.getCodeHash().equals(codeHash)) {
                recoveryCode.setUsed(true);
                recoveryCode.setUsedAt(Instant.now());
                totpRecoveryCodeRepository.save(recoveryCode);

                auditPublisher.publish(AuditEventBuilder.builder()
                        .actorId(userId)
                        .action(AuditAction.MFA_RECOVERY_USED)
                        .entityType(AuditEntityType.AUTH)
                        .entityId(userId)
                        .sourceModule("auth")
                        .details("Recovery code used")
                        .build());

                notificationPublisher.publishToUser(userId, NotificationEventBuilder.builder()
                        .titleKey("notification.security.mfa_recovery_used")
                        .bodyKey("notification.security.mfa_recovery_used.body")
                        .category(NotificationCategory.SECURITY)
                        .sourceModule("auth")
                        .relatedEntityId(userId)
                        .relatedEntityType("AUTH")
                        .build());

                return true;
            }
        }
        return false;
    }

    /**
     * Disables 2FA for a user after verifying password and TOTP code.
     */
    @Transactional
    public void disableTotp(UUID userId, String password, String code) {
        // Check TOTP is enabled before validating credentials
        TotpSecret totpSecret = totpSecretRepository.findByUserId(userId)
                .filter(TotpSecret::isEnabled)
                .orElseThrow(() -> new BadRequestException("Two-factor authentication is not enabled"));

        UserPrincipal principal = userDetailsPort.loadById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (!passwordEncoder.matches(password, principal.getPassword())) {
            throw new UnauthorizedException("Invalid password");
        }

        String base32Secret = encryptionService.decrypt(totpSecret.getEncryptedSecret());
        if (!verifyTotpCode(base32Secret, code, totpSecret)) {
            throw new UnauthorizedException("Invalid verification code");
        }

        totpRecoveryCodeRepository.deleteByUserId(userId);
        totpSecretRepository.deleteByUserId(userId);

        auditPublisher.publish(AuditEventBuilder.builder()
                .actorId(userId)
                .action(AuditAction.MFA_DISABLED)
                .entityType(AuditEntityType.AUTH)
                .entityId(userId)
                .sourceModule("auth")
                .build());

        notificationPublisher.publishToUser(userId, NotificationEventBuilder.builder()
                .titleKey("notification.security.mfa_disabled")
                .bodyKey("notification.security.mfa_disabled.body")
                .category(NotificationCategory.SECURITY)
                .sourceModule("auth")
                .relatedEntityId(userId)
                .relatedEntityType("AUTH")
                .build());
    }

    /**
     * Regenerates recovery codes after verifying a current TOTP code.
     */
    @Transactional
    public RecoveryCodesResponse regenerateRecoveryCodes(UUID userId, String code) {
        if (!verifyCode(userId, code)) {
            throw new UnauthorizedException("Invalid verification code");
        }

        totpRecoveryCodeRepository.deleteByUserId(userId);
        List<String> plainCodes = generateAndStoreRecoveryCodes(userId);

        auditPublisher.publish(AuditEventBuilder.builder()
                .actorId(userId)
                .action(AuditAction.MFA_RECOVERY_REGENERATED)
                .entityType(AuditEntityType.AUTH)
                .entityId(userId)
                .sourceModule("auth")
                .build());

        return new RecoveryCodesResponse(plainCodes);
    }

    /**
     * Returns the 2FA status for a user.
     */
    @Transactional(readOnly = true)
    public TotpStatusResponse getStatus(UUID userId) {
        return totpSecretRepository.findByUserId(userId)
                .filter(TotpSecret::isEnabled)
                .map(secret -> new TotpStatusResponse(
                        true,
                        totpRecoveryCodeRepository.countByUserIdAndUsedFalse(userId),
                        secret.getCreatedAt()))
                .orElse(new TotpStatusResponse(false, 0, null));
    }

    /**
     * Simple boolean check whether 2FA is enabled for a user.
     */
    public boolean isTotpEnabled(UUID userId) {
        return totpSecretRepository.existsByUserIdAndEnabledTrue(userId);
    }

    /**
     * Returns the count of users with TOTP enabled (for actuator info).
     */
    public long countUsersWithMfa() {
        return totpSecretRepository.countByEnabledTrue();
    }

    // --- Private helpers ---

    private boolean verifyTotpCode(String base32Secret, String code, TotpSecret totpSecret) {
        try {
            // TOTPGenerator internally base32-decodes the secret bytes to get the raw HMAC key
            byte[] secret = base32Secret.getBytes(StandardCharsets.US_ASCII);
            TOTPGenerator totp = new TOTPGenerator.Builder(secret)
                    .withHOTPGenerator(b -> {
                        b.withPasswordLength(totpSecret.getDigits());
                        b.withAlgorithm(HMACAlgorithm.SHA1);
                    })
                    .withPeriod(Duration.ofSeconds(totpSecret.getPeriod()))
                    .build();
            return totp.verify(code, 1); // allow 1 period clock skew
        } catch (Exception e) {
            log.debug("TOTP verification failed: {}", e.getMessage());
            return false;
        }
    }

    private String buildOtpauthUri(String base32Secret, String email, MfaProperties.TotpConfig config) {
        String issuer = URLEncoder.encode(config.getIssuer(), StandardCharsets.UTF_8);
        String label = URLEncoder.encode(config.getIssuer() + ":" + email, StandardCharsets.UTF_8);
        return URI.create(String.format(
                "otpauth://totp/%s?secret=%s&issuer=%s&algorithm=%s&digits=%d&period=%d",
                label, base32Secret, issuer, config.getAlgorithm(), config.getDigits(), config.getPeriod()
        )).toString();
    }

    private List<String> generateAndStoreRecoveryCodes(UUID userId) {
        totpRecoveryCodeRepository.deleteByUserId(userId);

        int count = mfaProperties.getRecoveryCodeCount();
        List<String> plainCodes = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            String code = generateRecoveryCode();
            plainCodes.add(code);

            String hash = AuthService.hashToken(code.replace("-", ""));
            TotpRecoveryCode entity = TotpRecoveryCode.builder()
                    .userId(userId)
                    .codeHash(hash)
                    .build();
            totpRecoveryCodeRepository.save(entity);
        }

        return plainCodes;
    }

    private String generateRecoveryCode() {
        StringBuilder sb = new StringBuilder(9); // XXXX-XXXX
        for (int i = 0; i < 8; i++) {
            if (i == 4) sb.append('-');
            sb.append(RECOVERY_CODE_CHARS.charAt(SECURE_RANDOM.nextInt(RECOVERY_CODE_CHARS.length())));
        }
        return sb.toString();
    }

}
