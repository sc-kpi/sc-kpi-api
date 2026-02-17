-- TOTP secrets table for storing encrypted 2FA secrets
CREATE TABLE totp_secrets
(
    id               UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id          UUID         NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    encrypted_secret TEXT         NOT NULL,
    algorithm        VARCHAR(10)  DEFAULT 'SHA1' NOT NULL,
    digits           INT          DEFAULT 6 NOT NULL,
    period           INT          DEFAULT 30 NOT NULL,
    enabled          BOOLEAN      DEFAULT FALSE NOT NULL,
    created_at       TIMESTAMP(6) DEFAULT now() NOT NULL,
    updated_at       TIMESTAMP(6) DEFAULT now() NOT NULL
);

CREATE INDEX idx_totp_secrets_user_id ON totp_secrets (user_id);

-- TOTP recovery codes table for storing hashed backup codes
CREATE TABLE totp_recovery_codes
(
    id         UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id    UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    code_hash  VARCHAR(64)  NOT NULL,
    used       BOOLEAN      DEFAULT FALSE NOT NULL,
    used_at    TIMESTAMP(6),
    created_at TIMESTAMP(6) DEFAULT now() NOT NULL
);

CREATE INDEX idx_totp_recovery_codes_user_id ON totp_recovery_codes (user_id);
CREATE INDEX idx_totp_recovery_codes_user_id_used ON totp_recovery_codes (user_id, used);
