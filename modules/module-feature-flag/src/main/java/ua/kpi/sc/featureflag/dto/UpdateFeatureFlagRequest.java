package ua.kpi.sc.featureflag.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateFeatureFlagRequest(
        @Size(max = 255) String name,
        String description,
        Boolean enabled,
        String environment,
        @Min(0) @Max(100) Integer rolloutPercentage
) {
}
