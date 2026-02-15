package ua.kpi.sc.featureflag.dto;

import jakarta.validation.constraints.NotNull;

public record ToggleFeatureFlagRequest(
        @NotNull Boolean enabled,
        String reason
) {
}
