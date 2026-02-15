package ua.kpi.sc.audit.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import ua.kpi.sc.audit.entity.AuditEventEntity;
import ua.kpi.sc.audit.repository.AuditEventRepository;
import ua.kpi.sc.common.audit.AuditEvent;

@ExtendWith(MockitoExtension.class)
class AuditPublisherImplTest {

    @Mock
    private AuditEventRepository repository;

    @Mock
    private PlatformTransactionManager transactionManager;

    private AuditPublisherImpl publisher;

    @BeforeEach
    void setUp() {
        publisher = new AuditPublisherImpl(repository, transactionManager);
        lenient().when(transactionManager.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(mock(TransactionStatus.class));
    }

    @Test
    void publish_savesEntityWithAllFields() {
        UUID actorId = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();
        Instant ts = Instant.now();

        var event = new AuditEvent(
                actorId, "admin@kpi.ua", "CREATED", "USER",
                entityId, "test@kpi.ua", null, null, null,
                "New user", "user", "10.0.0.1", ts
        );

        publisher.publish(event);

        verify(repository).save(argThat(entity ->
                entity.getActorId().equals(actorId) &&
                entity.getActorEmail().equals("admin@kpi.ua") &&
                entity.getAction().equals("CREATED") &&
                entity.getEntityType().equals("USER") &&
                entity.getEntityId().equals(entityId) &&
                entity.getEntityName().equals("test@kpi.ua") &&
                entity.getDetails().equals("New user") &&
                entity.getSourceModule().equals("user") &&
                entity.getIpAddress().equals("10.0.0.1") &&
                entity.getCreatedAt().equals(ts)
        ));
    }

    @Test
    void publish_handlesNullFields() {
        var event = new AuditEvent(
                null, null, "LOGIN_FAILED", "AUTH",
                null, "unknown@kpi.ua", null, null, null,
                null, "auth", null, null
        );

        publisher.publish(event);

        verify(repository).save(argThat(entity ->
                entity.getActorId() == null &&
                entity.getAction().equals("LOGIN_FAILED") &&
                entity.getEntityType().equals("AUTH") &&
                entity.getCreatedAt() != null  // auto-populated by record
        ));
    }

    @Test
    void publish_doesNotPropagateException_fromRepository() {
        when(repository.save(any(AuditEventEntity.class)))
                .thenThrow(new RuntimeException("DB failure"));

        var event = new AuditEvent(
                null, null, "CREATED", "USER",
                null, "test@kpi.ua", null, null, null,
                null, "user", null, null
        );

        assertThatCode(() -> publisher.publish(event)).doesNotThrowAnyException();
    }

    @Test
    void publish_doesNotPropagateException_fromTransactionManager() {
        when(transactionManager.getTransaction(any(TransactionDefinition.class)))
                .thenThrow(new RuntimeException("Connection pool exhausted"));

        var event = new AuditEvent(
                null, null, "LOGIN", "AUTH",
                null, "user@kpi.ua", null, null, null,
                null, "auth", null, null
        );

        assertThatCode(() -> publisher.publish(event)).doesNotThrowAnyException();
    }
}
