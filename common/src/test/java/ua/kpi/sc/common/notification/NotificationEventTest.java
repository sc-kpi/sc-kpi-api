package ua.kpi.sc.common.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class NotificationEventTest {

    @Test
    void timestampDefaultsToNow_whenNull() {
        Instant before = Instant.now();
        var event = new NotificationEvent(
                "title.key", "body.key", null,
                NotificationCategory.SECURITY, "auth",
                null, null, null
        );
        assertThat(event.timestamp()).isAfterOrEqualTo(before);
        assertThat(event.timestamp()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void timestampPreserved_whenProvided() {
        Instant custom = Instant.parse("2025-01-01T00:00:00Z");
        var event = new NotificationEvent(
                "title.key", "body.key", null,
                NotificationCategory.SYSTEM, "notification",
                null, null, custom
        );
        assertThat(event.timestamp()).isEqualTo(custom);
    }

    @Test
    void bodyArgsDefaultsToEmptyArray_whenNull() {
        var event = new NotificationEvent(
                "title.key", "body.key", null,
                NotificationCategory.ADMIN, "user",
                null, null, null
        );
        assertThat(event.bodyArgs()).isNotNull().isEmpty();
    }

    @Test
    void bodyArgsPreserved_whenProvided() {
        String[] args = {"arg1", "arg2"};
        var event = new NotificationEvent(
                "title.key", "body.key", args,
                NotificationCategory.ADMIN, "user",
                null, null, null
        );
        assertThat(event.bodyArgs()).containsExactly("arg1", "arg2");
    }

    @Test
    void allFieldsAreAccessible() {
        UUID entityId = UUID.randomUUID();
        Instant ts = Instant.now();
        String[] args = {"BASIC", "ADMIN"};

        var event = new NotificationEvent(
                "notification.admin.tier_changed",
                "notification.admin.tier_changed.body",
                args,
                NotificationCategory.ADMIN,
                "user",
                entityId,
                "USER",
                ts
        );

        assertThat(event.titleKey()).isEqualTo("notification.admin.tier_changed");
        assertThat(event.bodyKey()).isEqualTo("notification.admin.tier_changed.body");
        assertThat(event.bodyArgs()).containsExactly("BASIC", "ADMIN");
        assertThat(event.category()).isEqualTo(NotificationCategory.ADMIN);
        assertThat(event.sourceModule()).isEqualTo("user");
        assertThat(event.relatedEntityId()).isEqualTo(entityId);
        assertThat(event.relatedEntityType()).isEqualTo("USER");
        assertThat(event.timestamp()).isEqualTo(ts);
    }
}
