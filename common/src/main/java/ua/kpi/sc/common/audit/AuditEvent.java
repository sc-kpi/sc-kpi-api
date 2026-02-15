package ua.kpi.sc.common.audit;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable audit event record published by modules and persisted by the audit system.
 *
 * @param actorId      user performing the action (null for anonymous/system)
 * @param actorEmail   denormalized for query convenience
 * @param action       audit action type
 * @param entityType   type of entity being audited
 * @param entityId     affected entity UUID (null for auth events)
 * @param entityName   human-readable identifier (email, flag key)
 * @param fieldName    specific field changed (null unless UPDATED)
 * @param oldValue     previous value as string
 * @param newValue     new value as string
 * @param details      additional context/reason
 * @param sourceModule originating module: "auth", "user", "feature-flag"
 * @param ipAddress    client IP (null when unavailable)
 * @param timestamp    event timestamp, defaults to now
 * @since 0.4.0
 */
public record AuditEvent(
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
        Instant timestamp
) {

    public AuditEvent {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }
}
