package ua.kpi.sc.ratelimit.dto;

import java.util.Map;

public record RateLimitStatsResponse(
        long totalViolations,
        long activeBuckets,
        Map<String, Long> violationsByEndpoint
) {
}
