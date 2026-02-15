package ua.kpi.sc.featureflag.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateFeatureFlagRequest(
        @NotBlank @Size(max = 255) @Pattern(regexp = "^[a-z][a-z0-9.\\-]*$",
                message = "Key must start with a lowercase letter and contain only lowercase letters, digits, dots, and hyphens")
        String key,
        @NotBlank @Size(max = 255) String name,
        String description,
        boolean enabled,
        String environment,
        @Min(0) @Max(100) int rolloutPercentage
) {
}
