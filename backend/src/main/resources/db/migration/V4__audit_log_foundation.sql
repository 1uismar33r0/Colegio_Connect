-- Issue #15: minimal audit foundation for administrative and sponsorship actions.

CREATE TABLE audit_log (
    id          UUID         PRIMARY KEY DEFAULT uuidv7(),
    actor_id    UUID         NOT NULL REFERENCES users (id),
    action      VARCHAR(40)  NOT NULL,
    target_type VARCHAR(30)  NOT NULL,
    target_id   UUID         NOT NULL,
    occurred_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_audit_log_action CHECK (action IN (
        'ROLE_ASSIGNED',
        'ROLE_REVOKED',
        'ORGANIZATION_APPROVED',
        'ORGANIZATION_REJECTED',
        'LISTING_HIDDEN',
        'LISTING_REMOVED',
        'ACCOUNT_SUSPENDED',
        'SPONSORSHIP_ACCEPTED'
    )),
    CONSTRAINT ck_audit_log_target_type CHECK (target_type IN (
        'USER',
        'ORGANIZATION',
        'LISTING',
        'SPONSORSHIP_REQUEST'
    ))
);

CREATE INDEX idx_audit_log_occurred_at ON audit_log (occurred_at DESC);
