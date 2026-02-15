package ua.kpi.sc.audit.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(
        UUID id,
        UUID actorId,
        String actorEmail,
        String action,
        String entityType,
        UUID entityId,
        String entityName,
        String fieldName,
        String oldValue,
        String newValue,
        String details,
        String sourceModule,
        String ipAddress,
        Instant createdAt
) {
}
