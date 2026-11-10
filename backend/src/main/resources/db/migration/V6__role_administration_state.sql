-- Issue #10: serialize administrator role changes and make initial bootstrap one-time.
CREATE TABLE role_admin_state (
    id                     SMALLINT PRIMARY KEY CHECK (id = 1),
    bootstrap_completed_at TIMESTAMPTZ,
    bootstrap_user_id      UUID
);

INSERT INTO role_admin_state (id) VALUES (1);

-- Exact case-normalized administrator lookup without scanning every account.
CREATE INDEX idx_users_normalized_email ON users ((lower(email)));

-- Bootstrap has no human actor. Keep one audit stream with an explicit system actor.
ALTER TABLE audit_log ALTER COLUMN actor_id DROP NOT NULL;
ALTER TABLE audit_log ADD COLUMN actor_type VARCHAR(10) NOT NULL DEFAULT 'USER';
ALTER TABLE audit_log ADD CONSTRAINT ck_audit_actor CHECK (
    (actor_type = 'USER' AND actor_id IS NOT NULL)
    OR (actor_type = 'SYSTEM' AND actor_id IS NULL)
);
