package ua.kpi.sc.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the notification subsystem.
 *
 * @since 0.5.0
 */
@ConfigurationProperties(prefix = "app.notifications")
public record NotificationProperties(
        long sseTimeoutMs,
        long sseHeartbeatIntervalMs,
        int retentionDays,
        int emailMaxRetries,
        Cleanup cleanup
) {

    public NotificationProperties {
        if (sseTimeoutMs <= 0) sseTimeoutMs = 300_000;
        if (sseHeartbeatIntervalMs <= 0) sseHeartbeatIntervalMs = 30_000;
        if (retentionDays <= 0) retentionDays = 90;
        if (emailMaxRetries <= 0) emailMaxRetries = 3;
        if (cleanup == null) cleanup = new Cleanup(true, "0 0 3 * * ?");
    }

    public record Cleanup(boolean enabled, String cron) {
        public Cleanup {
            if (cron == null) cron = "0 0 3 * * ?";
        }
    }
}
