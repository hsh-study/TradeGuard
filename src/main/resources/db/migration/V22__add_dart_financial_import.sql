ALTER TABLE quarterly_financials
    MODIFY COLUMN free_cash_flow DECIMAL(19, 4);

CREATE TABLE dart_corp_mappings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    stock_code VARCHAR(20) NOT NULL,
    corp_code VARCHAR(20) NOT NULL,
    corp_name VARCHAR(255) NOT NULL,
    market VARCHAR(30) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_dart_corp_mapping_stock UNIQUE (stock_code),
    CONSTRAINT uk_dart_corp_mapping_corp UNIQUE (corp_code)
);

CREATE TABLE dart_financial_import_histories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    stock_code VARCHAR(20) NOT NULL,
    corp_code VARCHAR(20),
    fiscal_year INT NOT NULL,
    report_code VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    imported_quarterly_financial_count INT NOT NULL,
    failure_reason VARCHAR(1000),
    requested_at TIMESTAMP(6) NOT NULL,
    completed_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_dart_financial_import_history_stock
    ON dart_financial_import_histories (stock_code, requested_at);
