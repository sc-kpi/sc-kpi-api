package ua.kpi.sc.common.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class AuditEventTest {

    @Test
    void timestampDefaultsToNow_whenNull() {
        Instant before = Instant.now();
        var event = new AuditEvent(
                null, null, "CREATED", "USER", null, null,
                null, null, null, null, "user", null, null
        );
        assertThat(event.timestamp()).isAfterOrEqualTo(before);
        assertThat(event.timestamp()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void timestampPreserved_whenProvided() {
        Instant custom = Instant.parse("2025-01-01T00:00:00Z");
        var event = new AuditEvent(
                null, null, "CREATED", "USER", null, null,
                null, null, null, null, "user", null, custom
        );
        assertThat(event.timestamp()).isEqualTo(custom);
    }

    @Test
    void allFieldsAreAccessible() {
        UUID actorId = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();
        Instant ts = Instant.now();

        var event = new AuditEvent(
                actorId, "admin@kpi.ua", "UPDATED", "FEATURE_FLAG",
                entityId, "test.flag", "enabled", "true", "false",
                "Emergency disable", "feature-flag", "127.0.0.1", ts
        );

        assertThat(event.actorId()).isEqualTo(actorId);
        assertThat(event.actorEmail()).isEqualTo("admin@kpi.ua");
        assertThat(event.action()).isEqualTo("UPDATED");
        assertThat(event.entityType()).isEqualTo("FEATURE_FLAG");
        assertThat(event.entityId()).isEqualTo(entityId);
        assertThat(event.entityName()).isEqualTo("test.flag");
        assertThat(event.fieldName()).isEqualTo("enabled");
        assertThat(event.oldValue()).isEqualTo("true");
        assertThat(event.newValue()).isEqualTo("false");
        assertThat(event.details()).isEqualTo("Emergency disable");
        assertThat(event.sourceModule()).isEqualTo("feature-flag");
        assertThat(event.ipAddress()).isEqualTo("127.0.0.1");
        assertThat(event.timestamp()).isEqualTo(ts);
    }
}
