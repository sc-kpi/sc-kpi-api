package ua.kpi.sc.common.notification;

import java.time.Instant;
import java.util.UUID;

/**
 * Fluent builder for {@link NotificationEvent}.
 *
 * @since 0.5.0
 */
public final class NotificationEventBuilder {

    private String titleKey;
    private String bodyKey;
    private String[] bodyArgs;
    private NotificationCategory category;
    private String sourceModule;
    private UUID relatedEntityId;
    private String relatedEntityType;
    private Instant timestamp;

    private NotificationEventBuilder() {
    }

    public static NotificationEventBuilder builder() {
        return new NotificationEventBuilder();
    }

    public NotificationEventBuilder titleKey(String titleKey) {
        this.titleKey = titleKey;
        return this;
    }

    public NotificationEventBuilder bodyKey(String bodyKey) {
        this.bodyKey = bodyKey;
        return this;
    }

    public NotificationEventBuilder bodyArgs(String... bodyArgs) {
        this.bodyArgs = bodyArgs;
        return this;
    }

    public NotificationEventBuilder category(NotificationCategory category) {
        this.category = category;
        return this;
    }

    public NotificationEventBuilder sourceModule(String sourceModule) {
        this.sourceModule = sourceModule;
        return this;
    }

    public NotificationEventBuilder relatedEntityId(UUID relatedEntityId) {
        this.relatedEntityId = relatedEntityId;
        return this;
    }

    public NotificationEventBuilder relatedEntityType(String relatedEntityType) {
        this.relatedEntityType = relatedEntityType;
        return this;
    }

    public NotificationEventBuilder timestamp(Instant timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public NotificationEvent build() {
        return new NotificationEvent(
                titleKey, bodyKey, bodyArgs, category, sourceModule,
                relatedEntityId, relatedEntityType, timestamp
        );
    }
}
