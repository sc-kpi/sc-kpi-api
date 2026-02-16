CREATE TABLE notifications (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title_key           VARCHAR(200) NOT NULL,
    body_key            VARCHAR(200) NOT NULL,
    body_args           TEXT[],
    category            VARCHAR(30) NOT NULL,
    source_module       VARCHAR(50) NOT NULL,
    related_entity_id   UUID,
    related_entity_type VARCHAR(50),
    is_read             BOOLEAN NOT NULL DEFAULT FALSE,
    read_at             TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_user_read_created
    ON notifications(user_id, is_read, created_at DESC);

CREATE INDEX idx_notifications_user_unread
    ON notifications(user_id) WHERE is_read = FALSE;

CREATE INDEX idx_notifications_created_at
    ON notifications(created_at);

CREATE INDEX idx_notifications_category
    ON notifications(category);


CREATE TABLE notification_preferences (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category    VARCHAR(30) NOT NULL,
    channel     VARCHAR(20) NOT NULL,
    enabled     BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uq_notif_pref UNIQUE (user_id, category, channel)
);
