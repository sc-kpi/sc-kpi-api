package ua.kpi.sc.auth.config;

import java.util.List;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externalized MFA configuration bound to the {@code app.mfa.*} property prefix.
 *
 * @since 0.6.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.mfa")
public class MfaProperties {

    private TotpConfig totp = new TotpConfig();
    private String encryptionKey;
    private long tokenExpiration = 300_000; // 5 minutes
    private int recoveryCodeCount = 10;
    private int maxVerificationAttempts = 5;
    private EnforcementConfig enforcement = new EnforcementConfig();

    @Data
    public static class TotpConfig {
        private String issuer = "SC-KPI";
        private String algorithm = "SHA1";
        private int digits = 6;
        private int period = 30;
    }

    @Data
    public static class EnforcementConfig {
        private int gracePeriodDays = 7;
        private List<Integer> requiredTiers = List.of(4, 5);
    }
}
