package ua.kpi.sc.audit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import ua.kpi.sc.audit.entity.AuditEventEntity;
import ua.kpi.sc.audit.repository.AuditEventRepository;
import ua.kpi.sc.common.audit.AuditEvent;
import ua.kpi.sc.common.audit.AuditPublisher;

/**
 * Persists audit events in an independent transaction.
 * Uses programmatic {@code TransactionTemplate} with {@code REQUIRES_NEW} propagation
 * inside a try-catch so that failures (including connection pool exhaustion) never
 * propagate to the calling service.
 *
 * @since 0.4.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditPublisherImpl implements AuditPublisher {

    private final AuditEventRepository repository;
    private final PlatformTransactionManager transactionManager;

    @Override
    public void publish(AuditEvent event) {
        try {
            var tx = new TransactionTemplate(transactionManager);
            tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            tx.executeWithoutResult(status -> repository.save(toEntity(event)));
        } catch (Exception e) {
            log.error("Failed to persist audit event: action={}, entityType={}, entityId={}",
                    event.action(), event.entityType(), event.entityId(), e);
        }
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
