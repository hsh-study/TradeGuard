CREATE TABLE dart_corp_code_import_histories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    status VARCHAR(20) NOT NULL,
    imported_count INT NOT NULL,
    matched_stock_count INT NOT NULL,
    failure_reason VARCHAR(1000),
    requested_at TIMESTAMP(6) NOT NULL,
    completed_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_dart_corp_code_import_history_requested_at
    ON dart_corp_code_import_histories (requested_at);

CREATE TABLE shares_outstanding_import_histories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    status VARCHAR(20) NOT NULL,
    imported_count INT NOT NULL,
    failure_reason VARCHAR(1000),
    requested_at TIMESTAMP(6) NOT NULL,
    completed_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_shares_outstanding_import_history_requested_at
    ON shares_outstanding_import_histories (requested_at);
