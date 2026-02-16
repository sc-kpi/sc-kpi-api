package ua.kpi.sc.notification.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO representing a single notification.
 *
 * @since 0.5.0
 */
public record NotificationResponse(
        UUID id,
        UUID userId,
        String titleKey,
        String bodyKey,
        String[] bodyArgs,
        String category,
        String sourceModule,
        UUID relatedEntityId,
        String relatedEntityType,
        boolean read,
        Instant readAt,
        Instant createdAt
) {
}
