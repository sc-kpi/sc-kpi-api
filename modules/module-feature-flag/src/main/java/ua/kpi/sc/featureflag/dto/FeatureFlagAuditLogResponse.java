package ua.kpi.sc.featureflag.dto;

import java.time.Instant;
import java.util.UUID;

public record FeatureFlagAuditLogResponse(
        UUID id,
        UUID flagId,
        String flagKey,
        String action,
        String fieldName,
        String oldValue,
        String newValue,
        String reason,
        UUID changedBy,
        Instant changedAt
) {
}
