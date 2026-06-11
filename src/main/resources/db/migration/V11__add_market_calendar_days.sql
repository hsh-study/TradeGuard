CREATE TABLE market_calendar_days (
    id BIGINT NOT NULL AUTO_INCREMENT,
    market VARCHAR(30) NOT NULL DEFAULT 'KRX_STOCK',
    trade_date DATE NOT NULL,
    trading_day BOOLEAN NOT NULL,
    holiday_name VARCHAR(200),
    source VARCHAR(30) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_market_calendar_market_trade_date UNIQUE (market, trade_date)
);

CREATE INDEX idx_market_calendar_trade_date
    ON market_calendar_days (trade_date);

CREATE INDEX idx_market_calendar_trading_day
    ON market_calendar_days (trading_day);

CREATE INDEX idx_market_calendar_source
    ON market_calendar_days (source);
