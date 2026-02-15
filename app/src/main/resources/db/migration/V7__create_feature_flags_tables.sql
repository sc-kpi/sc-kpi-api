CREATE TABLE feature_flags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    enabled BOOLEAN NOT NULL DEFAULT true,
    environment VARCHAR(50),
    rollout_percentage INT NOT NULL DEFAULT 100,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT chk_rollout_percentage CHECK (rollout_percentage >= 0 AND rollout_percentage <= 100)
);

CREATE INDEX idx_feature_flags_key ON feature_flags(key);
CREATE INDEX idx_feature_flags_enabled ON feature_flags(enabled);

CREATE TABLE feature_flag_overrides (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    flag_id UUID NOT NULL REFERENCES feature_flags(id) ON DELETE CASCADE,
    override_type VARCHAR(10) NOT NULL,
    tier_level INT,
    user_id UUID REFERENCES users(id),
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT chk_override_type CHECK (override_type IN ('TIER', 'USER')),
    CONSTRAINT chk_tier_override CHECK (
        (override_type = 'TIER' AND tier_level IS NOT NULL AND user_id IS NULL)
        OR (override_type = 'USER' AND user_id IS NOT NULL AND tier_level IS NULL)
    ),
    CONSTRAINT uq_flag_tier UNIQUE (flag_id, override_type, tier_level),
    CONSTRAINT uq_flag_user UNIQUE (flag_id, override_type, user_id)
);

CREATE INDEX idx_feature_flag_overrides_flag ON feature_flag_overrides(flag_id);

CREATE TABLE feature_flag_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    flag_id UUID REFERENCES feature_flags(id) ON DELETE CASCADE,
    flag_key VARCHAR(255) NOT NULL,
    action VARCHAR(50) NOT NULL,
    field_name VARCHAR(100),
    old_value TEXT,
    new_value TEXT,
    reason TEXT,
    changed_by UUID REFERENCES users(id),
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_feature_flag_audit_flag ON feature_flag_audit_log(flag_id);
CREATE INDEX idx_feature_flag_audit_changed_at ON feature_flag_audit_log(changed_at);
