CREATE TABLE indicator_warmup_histories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    stock_code VARCHAR(255) NOT NULL,
    base_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    imported_daily_price_count INT NOT NULL,
    total_daily_price_count INT NOT NULL,
    sufficient_for_ma20 BOOLEAN NOT NULL,
    sufficient_for_ma60 BOOLEAN NOT NULL,
    failure_reason VARCHAR(1000),
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_indicator_warmup_history_stock_created
    ON indicator_warmup_histories (stock_code, created_at);

CREATE INDEX idx_indicator_warmup_history_base_status
    ON indicator_warmup_histories (base_date, status);
