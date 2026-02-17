-- Seed the auth.mfa.totp feature flag (enabled by default, 100% rollout)
INSERT INTO feature_flags (id, key, enabled, rollout_percentage, description, created_at, updated_at)
VALUES (gen_random_uuid(), 'auth.mfa.totp', TRUE, 100,
        'Enables TOTP two-factor authentication endpoints', now(), now());
