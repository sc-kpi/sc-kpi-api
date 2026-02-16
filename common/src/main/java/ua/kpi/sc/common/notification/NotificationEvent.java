package ua.kpi.sc.common.notification;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable notification event record published by modules and delivered by the notification system.
 *
 * @param titleKey          i18n message key for the notification title
 * @param bodyKey           i18n message key for the notification body
 * @param bodyArgs          interpolation arguments for the body template
 * @param category          notification category for filtering and preferences
 * @param sourceModule      originating module: "auth", "user", "feature-flag", "notification"
 * @param relatedEntityId   optional UUID of the related domain entity
 * @param relatedEntityType optional type of the related entity (e.g. "USER", "FEATURE_FLAG")
 * @param timestamp         event timestamp, defaults to now
 * @since 0.5.0
 */
public record NotificationEvent(
        String titleKey,
        String bodyKey,
        String[] bodyArgs,
        NotificationCategory category,
        String sourceModule,
        UUID relatedEntityId,
        String relatedEntityType,
        Instant timestamp
) {

    public NotificationEvent {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
        if (bodyArgs == null) {
            bodyArgs = new String[0];
        }
    }
}
