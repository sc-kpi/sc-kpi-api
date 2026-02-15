package ua.kpi.sc.featureflag.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import ua.kpi.sc.featureflag.entity.OverrideType;

public record CreateOverrideRequest(
        @NotNull OverrideType overrideType,
        Integer tierLevel,
        UUID userId,
        @NotNull Boolean enabled
) {
}
