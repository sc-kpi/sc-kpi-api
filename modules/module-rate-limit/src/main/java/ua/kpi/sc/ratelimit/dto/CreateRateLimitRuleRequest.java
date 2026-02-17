package ua.kpi.sc.ratelimit.dto;

import java.time.LocalTime;
import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ua.kpi.sc.ratelimit.entity.RateLimitScope;

public record CreateRateLimitRuleRequest(
        @NotBlank @Size(max = 255) @Pattern(regexp = "^[a-z][a-z0-9.\\-]*$",
                message = "Name must start with a lowercase letter and contain only lowercase letters, digits, dots, and hyphens")
        String name,
        String description,
        @NotBlank @Size(max = 500) String endpointPattern,
        String httpMethod,
        @Min(1) int limitPerPeriod,
        @Min(1) int periodSeconds,
        @Min(1) int burstCapacity,
        @NotNull RateLimitScope scope,
        @Min(0) @Max(5) Integer targetTier,
        UUID targetUserId,
        LocalTime timeWindowStart,
        LocalTime timeWindowEnd,
        int priority,
        boolean enabled
) {
}
