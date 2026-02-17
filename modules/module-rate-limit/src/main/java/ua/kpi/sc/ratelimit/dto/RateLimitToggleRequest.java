package ua.kpi.sc.ratelimit.dto;

import jakarta.validation.constraints.NotNull;

public record RateLimitToggleRequest(
        @NotNull Boolean enabled
) {
}
