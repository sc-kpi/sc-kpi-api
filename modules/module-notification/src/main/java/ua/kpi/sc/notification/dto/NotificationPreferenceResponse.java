package ua.kpi.sc.notification.dto;

/**
 * Response DTO representing a single notification preference.
 *
 * @since 0.5.0
 */
public record NotificationPreferenceResponse(
        String category,
        String channel,
        boolean enabled
) {
}
