package ua.kpi.sc.featureflag.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FeatureFlagResponse(
        UUID id,
        String key,
        String name,
        String description,
        boolean enabled,
        String environment,
        int rolloutPercentage,
        List<OverrideResponse> overrides,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt
) {
}
