CREATE TABLE market_calendar_day_audits (
    id BIGINT NOT NULL AUTO_INCREMENT,
    market VARCHAR(30) NOT NULL,
    trade_date DATE NOT NULL,
    before_trading_day BOOLEAN,
    after_trading_day BOOLEAN NOT NULL,
    before_holiday_name VARCHAR(200),
    after_holiday_name VARCHAR(200),
    reason VARCHAR(500) NOT NULL,
    actor VARCHAR(100) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_market_calendar_audit_trade_date
    ON market_calendar_day_audits (trade_date);

CREATE INDEX idx_market_calendar_audit_created_at
    ON market_calendar_day_audits (created_at);
