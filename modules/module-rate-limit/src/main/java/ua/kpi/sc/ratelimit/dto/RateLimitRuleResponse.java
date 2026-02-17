package ua.kpi.sc.ratelimit.dto;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

import ua.kpi.sc.ratelimit.entity.RateLimitScope;

public record RateLimitRuleResponse(
        UUID id,
        String name,
        String description,
        String endpointPattern,
        String httpMethod,
        int limitPerPeriod,
        int periodSeconds,
        int burstCapacity,
        RateLimitScope scope,
        Integer targetTier,
        UUID targetUserId,
        LocalTime timeWindowStart,
        LocalTime timeWindowEnd,
        int priority,
        boolean enabled,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt
) {
}
