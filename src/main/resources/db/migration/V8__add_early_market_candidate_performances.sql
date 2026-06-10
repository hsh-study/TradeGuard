CREATE TABLE early_market_candidate_performances (
    id BIGINT NOT NULL AUTO_INCREMENT,
    signal_id BIGINT NOT NULL,
    stock_code VARCHAR(255) NOT NULL,
    trade_date DATE NOT NULL,
    signal_type VARCHAR(255) NOT NULL,
    entry_reference_price DECIMAL(19, 4),
    high_until_0930 DECIMAL(19, 4),
    low_until_0930 DECIMAL(19, 4),
    price_at_0930 DECIMAL(19, 4),
    max_return_rate_until_0930 DECIMAL(19, 4),
    max_drawdown_rate_until_0930 DECIMAL(19, 4),
    vwap_broken BOOLEAN,
    captured_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_early_market_performance_signal UNIQUE (signal_id),
    CONSTRAINT fk_early_market_performance_signal
        FOREIGN KEY (signal_id) REFERENCES trading_signals (id)
);

CREATE INDEX idx_early_market_performance_trade_date
    ON early_market_candidate_performances (trade_date);

CREATE INDEX idx_early_market_performance_type_trade_date
    ON early_market_candidate_performances (signal_type, trade_date);
