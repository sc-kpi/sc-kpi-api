package ua.kpi.sc.common.audit;

import java.time.Instant;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import ua.kpi.sc.common.security.UserPrincipal;

/**
 * Fluent builder for {@link AuditEvent} that auto-populates actor info
 * from the current {@link SecurityContextHolder} when available.
 *
 * @since 0.4.0
 */
public final class AuditEventBuilder {

    private UUID actorId;
    private String actorEmail;
    private String action;
    private String entityType;
    private UUID entityId;
    private String entityName;
    private String fieldName;
    private String oldValue;
    private String newValue;
    private String details;
    private String sourceModule;
    private String ipAddress;
    private Instant timestamp;

    private AuditEventBuilder() {
        // Try to auto-populate actor from security context
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            this.actorId = principal.getId();
            this.actorEmail = principal.getEmail();
        }
    }

    public static AuditEventBuilder builder() {
        return new AuditEventBuilder();
    }

    public AuditEventBuilder actorId(UUID actorId) {
        this.actorId = actorId;
        return this;
    }

    public AuditEventBuilder actorEmail(String actorEmail) {
        this.actorEmail = actorEmail;
        return this;
    }

    public AuditEventBuilder actor(UserPrincipal principal) {
        if (principal != null) {
            this.actorId = principal.getId();
            this.actorEmail = principal.getEmail();
        }
        return this;
    }

    public AuditEventBuilder action(AuditAction action) {
        this.action = action.name();
        return this;
    }

    public AuditEventBuilder entityType(AuditEntityType entityType) {
        this.entityType = entityType.name();
        return this;
    }

    public AuditEventBuilder entityId(UUID entityId) {
        this.entityId = entityId;
        return this;
    }

    public AuditEventBuilder entityName(String entityName) {
        this.entityName = entityName;
        return this;
    }

    public AuditEventBuilder fieldName(String fieldName) {
        this.fieldName = fieldName;
        return this;
    }

    public AuditEventBuilder oldValue(String oldValue) {
        this.oldValue = oldValue;
        return this;
    }

    public AuditEventBuilder newValue(String newValue) {
        this.newValue = newValue;
        return this;
    }

    public AuditEventBuilder details(String details) {
        this.details = details;
        return this;
    }

    public AuditEventBuilder sourceModule(String sourceModule) {
        this.sourceModule = sourceModule;
        return this;
    }

    public AuditEventBuilder ipAddress(String ipAddress) {
        this.ipAddress = ipAddress;
        return this;
    }

    public AuditEventBuilder timestamp(Instant timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public AuditEvent build() {
        return new AuditEvent(
                actorId, actorEmail, action, entityType, entityId, entityName,
                fieldName, oldValue, newValue, details, sourceModule, ipAddress, timestamp
        );
    }
}
