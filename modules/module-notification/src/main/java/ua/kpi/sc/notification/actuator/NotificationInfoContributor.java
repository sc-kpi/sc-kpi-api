package ua.kpi.sc.notification.actuator;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;
import ua.kpi.sc.notification.repository.NotificationRepository;
import ua.kpi.sc.notification.sse.SseEmitterRegistry;

/**
 * Contributes notification stats to the {@code /actuator/info} endpoint.
 *
 * @since 0.5.0
 */
@Component
@RequiredArgsConstructor
public class NotificationInfoContributor implements InfoContributor {

    private final NotificationRepository notificationRepository;
    private final SseEmitterRegistry sseRegistry;

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("notifications", Map.of(
                "total", notificationRepository.count(),
                "last24h", notificationRepository.countByCreatedAtAfter(
                        Instant.now().minus(24, ChronoUnit.HOURS)),
                "activeConnections", sseRegistry.getActiveConnectionCount(),
                "activeUsers", sseRegistry.getActiveUserCount()
        ));
    }
}
