-- Canonical listing core for dashboard reads, based on proposal Figure 2 and
-- Issues #18/#59 and #19/#61. Publishing/write workflows remain separate.
CREATE TABLE capstone_teams (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    name VARCHAR(255) NOT NULL CHECK (length(trim(name)) > 0),
    owner_user_id UUID NOT NULL REFERENCES users (id)
);

CREATE TABLE listings (
    id                UUID PRIMARY KEY DEFAULT uuidv7(),
    publisher_user_id UUID NOT NULL REFERENCES users (id),
    organization_id   UUID REFERENCES organizations (id),
    capstone_team_id UUID REFERENCES capstone_teams (id),
    title             VARCHAR(255) NOT NULL CHECK (length(trim(title)) > 0),
    summary           TEXT NOT NULL CHECK (length(trim(summary)) > 0),
    opportunity_type  VARCHAR(30) NOT NULL CHECK (opportunity_type IN (
        'JOB', 'INTERNSHIP', 'COOP', 'RESEARCH_POSITION',
        'RESEARCH_PARTICIPATION', 'CAPSTONE'
    )),
    tags              TEXT[] NOT NULL DEFAULT '{}',
    status            VARCHAR(20) NOT NULL CHECK (status IN (
        'DRAFT', 'IN_REVIEW', 'PUBLISHED', 'REJECTED',
        'CLOSED', 'HIDDEN', 'REMOVED'
    )),
    trust_label       VARCHAR(40) NOT NULL CHECK (trust_label IN (
        'VERIFIED_UPRM_INSTITUTION', 'APPROVED_EXTERNAL_COMPANY',
        'VERIFIED_STUDENT_PROJECT'
    )),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    posted_at         TIMESTAMPTZ,
    closes_at         TIMESTAMPTZ,
    CONSTRAINT ck_listing_owner_trust CHECK (
        (organization_id IS NOT NULL AND capstone_team_id IS NULL
         AND trust_label IN ('VERIFIED_UPRM_INSTITUTION', 'APPROVED_EXTERNAL_COMPANY'))
        OR (organization_id IS NULL AND capstone_team_id IS NOT NULL
            AND trust_label = 'VERIFIED_STUDENT_PROJECT'
            AND opportunity_type = 'CAPSTONE')
    ),
    CONSTRAINT ck_published_listing_dates CHECK (
        status <> 'PUBLISHED' OR (posted_at IS NOT NULL AND closes_at IS NOT NULL)
    )
);

CREATE INDEX idx_listings_discovery ON listings (posted_at DESC, id DESC)
    WHERE status = 'PUBLISHED';
CREATE INDEX idx_listings_organization_status
    ON listings (organization_id, status, posted_at DESC, id DESC);
CREATE INDEX idx_listings_search ON listings USING GIN (
    to_tsvector('simple', title || ' ' || summary)
);

CREATE INDEX idx_listings_team_status ON listings (capstone_team_id, status);
