package ua.kpi.sc.notification.dto;

/**
 * Response DTO containing the count of unread notifications.
 *
 * @since 0.5.0
 */
public record UnreadCountResponse(
        long count
) {
}
