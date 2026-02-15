package ua.kpi.sc.audit.repository;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;
import ua.kpi.sc.audit.entity.AuditEventEntity;

public final class AuditEventSpecification {

    private AuditEventSpecification() {}

    public static Specification<AuditEventEntity> always() {
        return (root, query, cb) -> cb.conjunction();
    }

    public static Specification<AuditEventEntity> hasActorId(UUID actorId) {
        return (root, query, cb) -> cb.equal(root.get("actorId"), actorId);
    }

    public static Specification<AuditEventEntity> hasEntityType(String entityType) {
        return (root, query, cb) -> cb.equal(root.get("entityType"), entityType);
    }

    public static Specification<AuditEventEntity> hasAction(String action) {
        return (root, query, cb) -> cb.equal(root.get("action"), action);
    }

    public static Specification<AuditEventEntity> hasSourceModule(String sourceModule) {
        return (root, query, cb) -> cb.equal(root.get("sourceModule"), sourceModule);
    }

    public static Specification<AuditEventEntity> hasEntityId(UUID entityId) {
        return (root, query, cb) -> cb.equal(root.get("entityId"), entityId);
    }

    public static Specification<AuditEventEntity> createdAfter(Instant from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<AuditEventEntity> createdBefore(Instant to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }

    public static Specification<AuditEventEntity> searchByText(String search) {
        String pattern = "%" + search.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("actorEmail")), pattern),
                cb.like(cb.lower(root.get("entityName")), pattern),
                cb.like(cb.lower(root.get("details")), pattern)
        );
    }
}
