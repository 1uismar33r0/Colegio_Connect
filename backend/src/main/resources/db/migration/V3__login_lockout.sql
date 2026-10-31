-- V3: Login lockout after repeated failed attempts (#8)
ALTER TABLE users
    ADD COLUMN failed_login_attempts INTEGER     NOT NULL DEFAULT 0,
    ADD COLUMN locked_until          TIMESTAMPTZ;