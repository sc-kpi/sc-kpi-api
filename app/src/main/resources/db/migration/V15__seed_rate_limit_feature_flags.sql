-- Seed rate limiting feature flags
INSERT INTO feature_flags (id, key, enabled, rollout_percentage, description, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'rate-limiting.enabled', TRUE, 100, 'Master switch for rate limiting filter', now(), now()),
    (gen_random_uuid(), 'rate-limiting.admin', TRUE, 100, 'Admin endpoints for rate limit rule management', now(), now());
