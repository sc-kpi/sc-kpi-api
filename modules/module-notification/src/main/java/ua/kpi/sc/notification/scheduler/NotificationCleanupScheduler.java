package ua.kpi.sc.notification.scheduler;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ua.kpi.sc.notification.config.NotificationProperties;
import ua.kpi.sc.notification.service.NotificationService;

/**
 * Scheduled task for cleaning up old notifications based on retention policy.
 *
 * @since 0.5.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.notifications.cleanup", name = "enabled", havingValue = "true", matchIfMissing = true)
public class NotificationCleanupScheduler {

    private final NotificationService notificationService;
    private final NotificationProperties properties;

    @Scheduled(cron = "${app.notifications.cleanup.cron:0 0 3 * * ?}")
    public void cleanupOldNotifications() {
        int days = properties.retentionDays();
        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
        int deleted = notificationService.deleteOldNotifications(cutoff);
        log.info("Notification cleanup: deleted {} notifications older than {} days", deleted, days);
    }
}
