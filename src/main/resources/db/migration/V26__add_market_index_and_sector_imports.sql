CREATE TABLE market_index_import_histories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    provider VARCHAR(30) NOT NULL,
    trade_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    imported_count INT NOT NULL,
    failure_reason VARCHAR(1000),
    requested_at TIMESTAMP(6) NOT NULL,
    completed_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_market_index_import_histories_requested
    ON market_index_import_histories (requested_at);

CREATE TABLE sector_import_histories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    status VARCHAR(30) NOT NULL,
    imported_sector_count INT NOT NULL,
    imported_mapping_count INT NOT NULL,
    failure_reason VARCHAR(1000),
    requested_at TIMESTAMP(6) NOT NULL,
    completed_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_sector_import_histories_requested
    ON sector_import_histories (requested_at);
