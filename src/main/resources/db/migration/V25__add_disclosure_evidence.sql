CREATE TABLE catalyst_evidences (
    id BIGINT NOT NULL AUTO_INCREMENT,
    catalyst_id BIGINT,
    stock_code VARCHAR(20),
    evidence_type VARCHAR(40) NOT NULL,
    title VARCHAR(500) NOT NULL,
    summary TEXT NOT NULL,
    source_name VARCHAR(100),
    source_url VARCHAR(1000),
    source_published_at TIMESTAMP(6),
    confidence VARCHAR(20) NOT NULL,
    created_by VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_catalyst_evidence_catalyst_status
    ON catalyst_evidences (catalyst_id, status);

CREATE INDEX idx_catalyst_evidence_stock_status
    ON catalyst_evidences (stock_code, status, source_published_at);

CREATE TABLE disclosure_evidence_import_histories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    provider VARCHAR(20) NOT NULL,
    stock_code VARCHAR(20),
    from_date DATE NOT NULL,
    to_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    imported_count INT NOT NULL,
    failure_reason VARCHAR(1000),
    requested_at TIMESTAMP(6) NOT NULL,
    completed_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_disclosure_evidence_import_history_requested_at
    ON disclosure_evidence_import_histories (requested_at);
