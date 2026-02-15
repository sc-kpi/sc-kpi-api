package ua.kpi.sc.audit.service;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.kpi.sc.audit.repository.AuditEventRepository;
import ua.kpi.sc.common.audit.AuditEvent;

@ExtendWith(MockitoExtension.class)
class AuditPublisherImplTest {

    @Mock
    private AuditEventRepository repository;

    @InjectMocks
    private AuditPublisherImpl publisher;

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
}
