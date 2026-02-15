CREATE TABLE audit_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id        UUID REFERENCES users(id) ON DELETE SET NULL,
    actor_email     VARCHAR(255),
    action          VARCHAR(50)  NOT NULL,
    entity_type     VARCHAR(50)  NOT NULL,
    entity_id       UUID,
    entity_name     VARCHAR(500),
    field_name      VARCHAR(100),
    old_value       TEXT,
    new_value       TEXT,
    details         TEXT,
    source_module   VARCHAR(50)  NOT NULL,
    ip_address      VARCHAR(45),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_events_created_at    ON audit_events(created_at DESC);
CREATE INDEX idx_audit_events_actor_id      ON audit_events(actor_id);
CREATE INDEX idx_audit_events_entity_type   ON audit_events(entity_type);
CREATE INDEX idx_audit_events_action        ON audit_events(action);
CREATE INDEX idx_audit_events_entity_id     ON audit_events(entity_id);
CREATE INDEX idx_audit_events_type_action   ON audit_events(entity_type, action, created_at DESC);
