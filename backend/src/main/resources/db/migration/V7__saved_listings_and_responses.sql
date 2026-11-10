-- Shared persisted counts for proposal Issues #23/#65 and #31/#69.
CREATE TABLE saved_listings (
    user_id    UUID NOT NULL REFERENCES users (id),
    listing_id UUID NOT NULL REFERENCES listings (id),
    saved_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, listing_id)
);

CREATE TABLE listing_responses (
    id             UUID PRIMARY KEY DEFAULT uuidv7(),
    listing_id     UUID NOT NULL REFERENCES listings (id),
    user_id        UUID NOT NULL REFERENCES users (id),
    status         VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'WITHDRAWN')),
    shared_fields  JSONB NOT NULL DEFAULT '{}'::jsonb,
    submitted_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    seen_at        TIMESTAMPTZ,
    CONSTRAINT uk_listing_response_user UNIQUE (listing_id, user_id)
);

CREATE INDEX idx_listing_responses_user_status
    ON listing_responses (user_id, status);
CREATE INDEX idx_listing_responses_new_by_listing
    ON listing_responses (listing_id, submitted_at DESC, id DESC)
    WHERE status = 'ACTIVE' AND seen_at IS NULL;
