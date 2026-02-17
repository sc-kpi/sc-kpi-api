package ua.kpi.sc.ratelimit.dto;

import java.time.LocalTime;
import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import ua.kpi.sc.ratelimit.entity.RateLimitScope;

public record UpdateRateLimitRuleRequest(
        String description,
        @Size(max = 500) String endpointPattern,
        String httpMethod,
        @Min(1) Integer limitPerPeriod,
        @Min(1) Integer periodSeconds,
        @Min(1) Integer burstCapacity,
        RateLimitScope scope,
        @Min(0) @Max(5) Integer targetTier,
        UUID targetUserId,
        LocalTime timeWindowStart,
        LocalTime timeWindowEnd,
        Integer priority,
        Boolean enabled
) {
}
