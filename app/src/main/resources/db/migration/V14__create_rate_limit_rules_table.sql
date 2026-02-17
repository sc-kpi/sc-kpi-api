-- Rate limit rules table
CREATE TABLE rate_limit_rules (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL UNIQUE,
    description     TEXT,
    endpoint_pattern VARCHAR(500) NOT NULL,
    http_method     VARCHAR(10),
    limit_per_period INT NOT NULL,
    period_seconds  INT NOT NULL,
    burst_capacity  INT NOT NULL,
    scope           VARCHAR(20) NOT NULL CHECK (scope IN ('GLOBAL', 'IP', 'USER', 'TIER')),
    target_tier     INT CHECK (target_tier BETWEEN 0 AND 5),
    target_user_id  UUID,
    time_window_start TIME,
    time_window_end TIME,
    priority        INT NOT NULL DEFAULT 0,
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    created_by      UUID REFERENCES users(id),
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_rate_limit_rules_enabled ON rate_limit_rules (enabled);
CREATE INDEX idx_rate_limit_rules_scope ON rate_limit_rules (scope);
CREATE INDEX idx_rate_limit_rules_priority ON rate_limit_rules (priority DESC);

-- Seed default rules
INSERT INTO rate_limit_rules (name, description, endpoint_pattern, http_method, limit_per_period, period_seconds, burst_capacity, scope, priority, enabled)
VALUES
    ('auth.login.ip', 'Login rate limit per IP', '/api/v1/auth/login', 'POST', 5, 60, 10, 'IP', 100, TRUE),
    ('auth.register.ip', 'Registration rate limit per IP', '/api/v1/auth/register', 'POST', 3, 60, 5, 'IP', 100, TRUE),
    ('auth.forgot-password.ip', 'Password reset request rate limit per IP', '/api/v1/auth/forgot-password', 'POST', 3, 300, 5, 'IP', 100, TRUE),
    ('api.global.ip', 'Global API rate limit per IP', '/api/v1/**', NULL, 120, 60, 180, 'IP', 1, TRUE),
    ('api.global.user', 'Global API rate limit per authenticated user', '/api/v1/**', NULL, 300, 60, 450, 'USER', 2, TRUE);
