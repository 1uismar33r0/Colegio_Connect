-- V2: Fields for single-use codes, attempt limits and resend limits (#7)
ALTER TABLE email_codes
    ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN used_at    TIMESTAMPTZ,
    ADD COLUMN attempts   INTEGER     NOT NULL DEFAULT 0;