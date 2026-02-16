package ua.kpi.sc.notification.dto;

import java.util.Map;

/**
 * Response DTO containing notification statistics.
 *
 * @since 0.5.0
 */
public record NotificationStatsResponse(
        long total,
        long last24h,
        Map<String, Long> byCategory
) {
}
