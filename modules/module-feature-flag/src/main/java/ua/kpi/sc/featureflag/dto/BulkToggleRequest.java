package ua.kpi.sc.featureflag.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record BulkToggleRequest(
        @NotEmpty List<String> keys,
        @NotNull Boolean enabled,
        String reason
) {
}
