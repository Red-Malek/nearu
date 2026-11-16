-- V1: initial schema, generated from the NearU Supabase project (public schema).
-- Differences from Supabase: no FK to auth.users, no RLS, PostGIS spatial_ref_sys not included.

CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE governorates (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code         VARCHAR NOT NULL UNIQUE,
    name         VARCHAR NOT NULL,
    centroid_lat DOUBLE PRECISION,
    centroid_lng DOUBLE PRECISION,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE delegations (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    governorate_id UUID NOT NULL REFERENCES governorates(id),
    name           VARCHAR NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (governorate_id, name)
);

CREATE TABLE service_categories (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patent_id  UUID REFERENCES service_categories(id),
    slug       VARCHAR NOT NULL UNIQUE,
    name       VARCHAR NOT NULL,
    icon       VARCHAR,
    sort_order INTEGER NOT NULL DEFAULT 0,
    active     BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE users (
    id         UUID PRIMARY KEY,  -- Supabase auth user id (sub claim)
    phone      VARCHAR NOT NULL,
    full_name  VARCHAR NOT NULL,
    role       VARCHAR NOT NULL CHECK (role IN ('CUSTOMER', 'PROFESSIONAL', 'ADMIN')),
    status     VARCHAR NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED')),
    locale     VARCHAR NOT NULL DEFAULT 'fr',
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE professional_profiles (
    user_id              UUID PRIMARY KEY REFERENCES users(id),
    display_name         VARCHAR NOT NULL,
    bio                  TEXT,
    years_experience     INTEGER NOT NULL DEFAULT 0 CHECK (years_experience >= 0),
    base_lat             DOUBLE PRECISION,
    base_lng             DOUBLE PRECISION,
    location             GEOGRAPHY GENERATED ALWAYS AS (
        CASE
            WHEN base_lat IS NOT NULL AND base_lng IS NOT NULL
            THEN (ST_SetSRID(ST_MakePoint(base_lng, base_lat), 4326))::geography
            ELSE NULL::geography
        END
    ) STORED,
    governorate_id       UUID REFERENCES governorates(id),
    address_line         VARCHAR,
    service_radius_km    INTEGER NOT NULL DEFAULT 10 CHECK (service_radius_km BETWEEN 1 AND 100),
    accepting_requests   BOOLEAN NOT NULL DEFAULT true,
    verification_status  VARCHAR NOT NULL DEFAULT 'UNVERIFIED'
        CHECK (verification_status IN ('UNVERIFIED', 'PENDING', 'VERIFIED', 'REJECTED')),
    verified_at          TIMESTAMPTZ,
    avg_rating           NUMERIC NOT NULL DEFAULT 0 CHECK (avg_rating >= 0 AND avg_rating <= 5),
    review_count         INTEGER NOT NULL DEFAULT 0 CHECK (review_count >= 0),
    completed_jobs_count INTEGER NOT NULL DEFAULT 0 CHECK (completed_jobs_count >= 0),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE professional_services (
    professional_id UUID NOT NULL REFERENCES professional_profiles(user_id),
    category_id     UUID NOT NULL REFERENCES service_categories(id),
    starting_price  NUMERIC,
    description     TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (professional_id, category_id)
);

CREATE TABLE stored_files (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id      UUID NOT NULL REFERENCES users(id),
    purpose       VARCHAR NOT NULL,
    storage_key   VARCHAR NOT NULL UNIQUE,
    original_name VARCHAR NOT NULL,
    content_type  VARCHAR NOT NULL,
    size_bytes    BIGINT NOT NULL CHECK (size_bytes > 0),
    sha256        VARCHAR,
    visibility    VARCHAR NOT NULL CHECK (visibility IN ('PUBLIC', 'PRIVATE')),
    deleted_at    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE portfolio_photos (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    professional_id UUID NOT NULL REFERENCES professional_profiles(user_id),
    file_id         UUID NOT NULL REFERENCES stored_files(id),
    caption         VARCHAR,
    sort_order      INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE verification_requests (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    professional_id  UUID NOT NULL REFERENCES professional_profiles(user_id),
    status           VARCHAR NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    submitted_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    reviewed_by      UUID REFERENCES users(id),
    reviewed_at      TIMESTAMPTZ,
    rejection_reason TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE verification_documents (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    verification_request_id UUID NOT NULL REFERENCES verification_requests(id),
    type                    VARCHAR NOT NULL CHECK (type IN ('ID_FRONT', 'ID_BACK', 'TRADE_CERT', 'OTHER')),
    file_id                 UUID NOT NULL REFERENCES stored_files(id),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE service_requests (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id         UUID NOT NULL REFERENCES users(id),
    professional_id     UUID NOT NULL REFERENCES professional_profiles(user_id),
    category_id         UUID NOT NULL REFERENCES service_categories(id),
    title               VARCHAR NOT NULL,
    description         TEXT NOT NULL,
    address_line        VARCHAR NOT NULL,
    governorate_id      UUID NOT NULL REFERENCES governorates(id),
    lat                 DOUBLE PRECISION,
    lng                 DOUBLE PRECISION,
    preferred_date      DATE,
    preferred_time_slot VARCHAR NOT NULL DEFAULT 'ANY'
        CHECK (preferred_time_slot IN ('MORNING', 'AFTERNOON', 'EVENING', 'ANY')),
    urgency             VARCHAR NOT NULL DEFAULT 'NORMAL' CHECK (urgency IN ('NORMAL', 'URGENT')),
    status              VARCHAR NOT NULL DEFAULT 'SUBMITTED'
        CHECK (status IN ('SUBMITTED', 'ACCEPTED', 'REJECTED', 'QUOTED', 'SCHEDULED', 'COMPLETED', 'CANCELLED', 'EXPIRED')),
    rejection_reason    TEXT,
    cancelled_by        UUID REFERENCES users(id),
    cancel_reason       TEXT,
    expires_at          TIMESTAMPTZ,
    quote_count         INTEGER NOT NULL DEFAULT 0 CHECK (quote_count >= 0),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE request_attachments (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id UUID NOT NULL REFERENCES service_requests(id),
    file_id    UUID NOT NULL REFERENCES stored_files(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE quotes (
    id                         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id                 UUID NOT NULL REFERENCES service_requests(id),
    professional_id            UUID NOT NULL REFERENCES professional_profiles(user_id),
    amount                     NUMERIC NOT NULL CHECK (amount > 0),
    proposed_start             TIMESTAMPTZ NOT NULL,
    estimated_duration_minutes INTEGER NOT NULL CHECK (estimated_duration_minutes > 0),
    valid_until                TIMESTAMPTZ NOT NULL CHECK (valid_until > now()),
    status                     VARCHAR NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'WITHDRAWN', 'EXPIRED', 'CANCELLED')),
    version                    INTEGER NOT NULL DEFAULT 0,
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                 TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE appointments (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_request_id UUID NOT NULL UNIQUE REFERENCES service_requests(id),
    quote_id           UUID NOT NULL UNIQUE REFERENCES quotes(id),
    customer_id        UUID NOT NULL REFERENCES users(id),
    professional_id    UUID NOT NULL REFERENCES professional_profiles(user_id),
    scheduled_start    TIMESTAMPTZ NOT NULL,
    scheduled_end      TIMESTAMPTZ NOT NULL,
    status             VARCHAR NOT NULL DEFAULT 'SCHEDULED' CHECK (status IN ('SCHEDULED', 'COMPLETED', 'CANCELLED')),
    completed_at       TIMESTAMPTZ,
    cancelled_by       UUID REFERENCES users(id),
    cancel_reason      TEXT,
    cancelled_at       TIMESTAMPTZ,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE status_history (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR NOT NULL,
    entity_id   UUID NOT NULL,
    from_status VARCHAR,
    to_status   VARCHAR NOT NULL,
    actor_id    UUID REFERENCES users(id),
    reason      TEXT,
    at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE conversations (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_request_id UUID NOT NULL UNIQUE REFERENCES service_requests(id),
    customer_id        UUID NOT NULL REFERENCES users(id),
    professional_id    UUID NOT NULL REFERENCES professional_profiles(user_id),
    last_message_at    TIMESTAMPTZ,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE messages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversations(id),
    sender_id       UUID NOT NULL REFERENCES users(id),
    body            VARCHAR NOT NULL CHECK (length(body) >= 1 AND length(body) <= 2000),
    sent_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    read_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE reviews (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id  UUID NOT NULL UNIQUE REFERENCES appointments(id),
    customer_id     UUID NOT NULL REFERENCES users(id),
    professional_id UUID NOT NULL REFERENCES professional_profiles(user_id),
    rating          SMALLINT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment         TEXT,
    status          VARCHAR NOT NULL DEFAULT 'PUBLISHED' CHECK (status IN ('PUBLISHED', 'HIDDEN')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE reports (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id UUID NOT NULL REFERENCES users(id),
    target_type VARCHAR NOT NULL
        CHECK (target_type IN ('USER', 'PROFILE', 'REVIEW', 'SERVICE_REQUEST', 'APPOINTMENT', 'MESSAGE')),
    target_id   UUID NOT NULL,
    reason      VARCHAR NOT NULL,
    details     TEXT,
    status      VARCHAR NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'IN_REVIEW', 'RESOLVED', 'DISMISSED')),
    resolved_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE audit_log (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id    UUID REFERENCES users(id),
    action      VARCHAR NOT NULL,
    entity_type VARCHAR NOT NULL,
    entity_id   UUID,
    metadata    JSONB,
    at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE notifications (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id UUID NOT NULL REFERENCES users(id),
    type         VARCHAR NOT NULL,
    entity_type  VARCHAR,
    entity_id    UUID,
    payload      JSONB,
    read_at      TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Indexes
CREATE INDEX idx_appointments_customer ON appointments (customer_id, status, scheduled_start DESC);
CREATE INDEX idx_appointments_professional ON appointments (professional_id, status, scheduled_start DESC);
CREATE INDEX idx_audit_log_action_at ON audit_log (action, at DESC);
CREATE INDEX idx_audit_log_actor ON audit_log (actor_id, at DESC);
CREATE INDEX idx_audit_log_entity ON audit_log (entity_type, entity_id);
CREATE INDEX idx_conversations_customer ON conversations (customer_id, last_message_at DESC);
CREATE INDEX idx_conversations_professional ON conversations (professional_id, last_message_at DESC);
CREATE INDEX idx_messages_conversation_sent ON messages (conversation_id, sent_at);
CREATE INDEX idx_notifications_recipient_read_created ON notifications (recipient_id, read_at, created_at DESC);
CREATE UNIQUE INDEX idx_one_pending_quote_per_request ON quotes (request_id) WHERE status = 'PENDING';
CREATE UNIQUE INDEX idx_one_pending_verification_per_professional ON verification_requests (professional_id) WHERE status = 'PENDING';
CREATE INDEX idx_portfolio_photos_professional ON portfolio_photos (professional_id);
CREATE INDEX idx_professional_profiles_governorate ON professional_profiles (governorate_id);
CREATE INDEX idx_professional_profiles_location ON professional_profiles USING gist (location);
CREATE INDEX idx_professional_services_category ON professional_services (category_id);
CREATE INDEX idx_quotes_professional ON quotes (professional_id, status, created_at DESC);
CREATE INDEX idx_reports_status_created ON reports (status, created_at);
CREATE INDEX idx_reports_target ON reports (target_type, target_id);
CREATE INDEX idx_request_attachments_request ON request_attachments (request_id);
CREATE INDEX idx_reviews_professional_status_created ON reviews (professional_id, status, created_at DESC);
CREATE INDEX idx_service_requests_customer ON service_requests (customer_id, status, created_at DESC);
CREATE INDEX idx_service_requests_expiry ON service_requests (status, expires_at);
CREATE INDEX idx_service_requests_professional ON service_requests (professional_id, status, created_at DESC);
CREATE INDEX idx_status_history_actor ON status_history (actor_id, at DESC);
CREATE INDEX idx_status_history_entity ON status_history (entity_type, entity_id, at DESC);
CREATE INDEX idx_stored_files_owner ON stored_files (owner_id);
CREATE INDEX idx_verification_documents_request ON verification_documents (verification_request_id);
