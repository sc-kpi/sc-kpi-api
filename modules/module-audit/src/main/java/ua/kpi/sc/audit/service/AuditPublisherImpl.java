package ua.kpi.sc.audit.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ua.kpi.sc.audit.entity.AuditEventEntity;
import ua.kpi.sc.audit.repository.AuditEventRepository;
import ua.kpi.sc.common.audit.AuditEvent;
import ua.kpi.sc.common.audit.AuditPublisher;

/**
 * Persists audit events in an independent transaction.
 * {@code REQUIRES_NEW} ensures events are saved even when the caller's transaction rolls back
 * (e.g. LOGIN_FAILED).
 *
 * @since 0.4.0
 */
@Service
@RequiredArgsConstructor
public class AuditPublisherImpl implements AuditPublisher {

    private final AuditEventRepository repository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publish(AuditEvent event) {
        repository.save(toEntity(event));
    }

    private AuditEventEntity toEntity(AuditEvent event) {
        return AuditEventEntity.builder()
                .actorId(event.actorId())
                .actorEmail(event.actorEmail())
                .action(event.action())
                .entityType(event.entityType())
                .entityId(event.entityId())
                .entityName(event.entityName())
                .fieldName(event.fieldName())
                .oldValue(event.oldValue())
                .newValue(event.newValue())
                .details(event.details())
                .sourceModule(event.sourceModule())
                .ipAddress(event.ipAddress())
                .createdAt(event.timestamp())
                .build();
    }
}
