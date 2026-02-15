package ua.kpi.sc.featureflag.dto;

import java.util.UUID;

import ua.kpi.sc.featureflag.entity.OverrideType;

public record OverrideResponse(
        UUID id,
        OverrideType overrideType,
        Integer tierLevel,
        UUID userId,
        boolean enabled
) {
}
