CREATE TABLE idempotency_records (
    id BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(200) NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    request_path VARCHAR(500) NOT NULL,
    client_scope VARCHAR(64) NOT NULL,
    request_fingerprint VARCHAR(64) NOT NULL,
    state VARCHAR(20) NOT NULL,
    response_status INTEGER,
    response_content_type VARCHAR(200),
    response_body TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_idempotency_scope UNIQUE (idempotency_key, http_method, request_path, client_scope)
);

CREATE INDEX idx_idempotency_expires_at ON idempotency_records(expires_at);
