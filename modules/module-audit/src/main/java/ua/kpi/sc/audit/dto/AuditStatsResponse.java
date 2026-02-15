package ua.kpi.sc.audit.dto;

import java.util.Map;

public record AuditStatsResponse(
        long totalEvents,
        long eventsLast24h,
        Map<String, Long> byEntityType,
        Map<String, Long> byAction
) {
}
