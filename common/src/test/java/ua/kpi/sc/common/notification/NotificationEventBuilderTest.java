package ua.kpi.sc.common.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class NotificationEventBuilderTest {

    @Test
    void buildsEventWithAllFields() {
        UUID entityId = UUID.randomUUID();
        Instant ts = Instant.now();

        NotificationEvent event = NotificationEventBuilder.builder()
                .titleKey("notification.security.login")
                .bodyKey("notification.security.login.body")
                .bodyArgs("user@kpi.ua", "Chrome")
                .category(NotificationCategory.SECURITY)
                .sourceModule("auth")
                .relatedEntityId(entityId)
                .relatedEntityType("AUTH")
                .timestamp(ts)
                .build();

        assertThat(event.titleKey()).isEqualTo("notification.security.login");
        assertThat(event.bodyKey()).isEqualTo("notification.security.login.body");
        assertThat(event.bodyArgs()).containsExactly("user@kpi.ua", "Chrome");
        assertThat(event.category()).isEqualTo(NotificationCategory.SECURITY);
        assertThat(event.sourceModule()).isEqualTo("auth");
        assertThat(event.relatedEntityId()).isEqualTo(entityId);
        assertThat(event.relatedEntityType()).isEqualTo("AUTH");
        assertThat(event.timestamp()).isEqualTo(ts);
    }

    @Test
    void timestampDefaultsToNow_whenNotSet() {
        Instant before = Instant.now();

        NotificationEvent event = NotificationEventBuilder.builder()
                .titleKey("test.title")
                .bodyKey("test.body")
                .category(NotificationCategory.SYSTEM)
                .sourceModule("notification")
                .build();

        assertThat(event.timestamp()).isAfterOrEqualTo(before);
        assertThat(event.timestamp()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void bodyArgsDefaultsToEmptyArray_whenNotSet() {
        NotificationEvent event = NotificationEventBuilder.builder()
                .titleKey("test.title")
                .bodyKey("test.body")
                .category(NotificationCategory.ADMIN)
                .sourceModule("user")
                .build();

        assertThat(event.bodyArgs()).isNotNull().isEmpty();
    }

    @Test
    void minimalEventCanBeBuilt() {
        NotificationEvent event = NotificationEventBuilder.builder()
                .titleKey("test.title")
                .bodyKey("test.body")
                .category(NotificationCategory.FEATURE_FLAG)
                .sourceModule("feature-flag")
                .build();

        assertThat(event.titleKey()).isEqualTo("test.title");
        assertThat(event.relatedEntityId()).isNull();
        assertThat(event.relatedEntityType()).isNull();
    }
}
