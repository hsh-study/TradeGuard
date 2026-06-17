ALTER TABLE valuation_snapshots
    ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'MANUAL' AFTER sales_per_share;

CREATE TABLE shares_outstanding_snapshots (
    id BIGINT NOT NULL AUTO_INCREMENT,
    stock_code VARCHAR(20) NOT NULL,
    base_date DATE NOT NULL,
    shares_outstanding DECIMAL(19, 4) NOT NULL,
    source VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_shares_outstanding_stock_date UNIQUE (stock_code, base_date)
);

CREATE INDEX idx_shares_outstanding_stock_date
    ON shares_outstanding_snapshots (stock_code, base_date);
