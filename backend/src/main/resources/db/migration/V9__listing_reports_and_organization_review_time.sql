-- Moderator review sources from the approved report workflow and existing
-- company approval. Historical UUIDv7 organization IDs retain their creation time.
ALTER TABLE organizations ADD COLUMN review_received_at TIMESTAMPTZ;
UPDATE organizations SET review_received_at = uuid_extract_timestamp(id)
 WHERE uuid_extract_version(id) = 7;
ALTER TABLE organizations ALTER COLUMN review_received_at SET DEFAULT CURRENT_TIMESTAMP;

-- Registration must persist this link when an external owner submits a company.
-- It grants only a pending, read-only dashboard; it is never a COMPANY role.
CREATE TABLE company_application_owners (
    organization_id UUID PRIMARY KEY REFERENCES organizations (id),
    user_id         UUID NOT NULL REFERENCES users (id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_company_application_owner_user
    ON company_application_owners (user_id, organization_id);

CREATE INDEX idx_pending_company_review_time
    ON organizations (review_received_at DESC, id DESC)
    WHERE org_type = 'EXTERNAL_COMPANY' AND approval_status = 'PENDING';

CREATE TABLE listing_reports (
    id               UUID PRIMARY KEY DEFAULT uuidv7(),
    listing_id       UUID NOT NULL REFERENCES listings (id),
    reporter_user_id UUID NOT NULL REFERENCES users (id),
    detail           VARCHAR(500) NOT NULL CHECK (length(trim(detail)) > 0),
    status           VARCHAR(20) NOT NULL CHECK (status IN ('OPEN', 'CLEARED')),
    received_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_listing_reporter UNIQUE (listing_id, reporter_user_id)
);

CREATE INDEX idx_open_listing_reports
    ON listing_reports (received_at DESC, id DESC)
    WHERE status = 'OPEN';
