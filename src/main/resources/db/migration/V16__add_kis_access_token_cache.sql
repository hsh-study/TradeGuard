CREATE TABLE kis_access_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    environment VARCHAR(20) NOT NULL,
    token_type VARCHAR(30),
    encrypted_access_token VARCHAR(4096),
    issued_at TIMESTAMP(6),
    expires_at TIMESTAMP(6),
    daily_issued_date DATE,
    refresh_started_at TIMESTAMP(6),
    refresh_owner VARCHAR(100),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_kis_access_token_environment UNIQUE (environment)
);

CREATE INDEX idx_kis_access_token_expires_at
    ON kis_access_tokens (expires_at);
CREATE INDEX idx_kis_access_token_refresh
    ON kis_access_tokens (refresh_started_at, refresh_owner);

INSERT INTO kis_access_tokens (
    environment, created_at, updated_at
) VALUES
    ('REAL', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('DEMO', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));
