CREATE TABLE early_market_follow_up_results (
    id BIGINT NOT NULL AUTO_INCREMENT,
    signal_id BIGINT NOT NULL,
    trade_date DATE NOT NULL,
    stock_code VARCHAR(255) NOT NULL,
    decision VARCHAR(32) NOT NULL,
    signal_score INT NOT NULL,
    last_price DECIMAL(19, 4),
    high_since_0905 DECIMAL(19, 4),
    drawdown_from_high DECIMAL(19, 4),
    vwap_broken BOOLEAN,
    reasons TEXT NOT NULL,
    captured_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_early_market_follow_up_signal UNIQUE (signal_id),
    CONSTRAINT fk_early_market_follow_up_signal
        FOREIGN KEY (signal_id) REFERENCES trading_signals (id)
);

CREATE INDEX idx_early_market_follow_up_trade_date
    ON early_market_follow_up_results (trade_date);

CREATE INDEX idx_early_market_follow_up_decision
    ON early_market_follow_up_results (decision);
