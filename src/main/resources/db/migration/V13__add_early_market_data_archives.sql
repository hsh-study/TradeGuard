CREATE TABLE early_market_data_captures (
    id BIGINT NOT NULL AUTO_INCREMENT,
    trade_date DATE NOT NULL,
    capture_type VARCHAR(50) NOT NULL,
    captured_at TIMESTAMP(6) NOT NULL,
    source VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    item_count INT NOT NULL,
    failure_reason VARCHAR(1000),
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_early_market_capture_date_type
        UNIQUE (trade_date, capture_type)
);

CREATE INDEX idx_early_market_capture_trade_date
    ON early_market_data_captures (trade_date);

CREATE INDEX idx_early_market_capture_status
    ON early_market_data_captures (status);

CREATE TABLE early_market_ranking_snapshots (
    id BIGINT NOT NULL AUTO_INCREMENT,
    trade_date DATE NOT NULL,
    captured_at TIMESTAMP(6) NOT NULL,
    rank_no INT NOT NULL,
    stock_code VARCHAR(20) NOT NULL,
    stock_name VARCHAR(100) NOT NULL,
    current_price DECIMAL(19, 4) NOT NULL,
    change_rate DECIMAL(19, 4) NOT NULL,
    volume BIGINT NOT NULL,
    trading_value DECIMAL(19, 4) NOT NULL,
    source VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_early_market_ranking_snapshot
        UNIQUE (trade_date, captured_at, source, rank_no, stock_code)
);

CREATE INDEX idx_early_market_ranking_trade_date
    ON early_market_ranking_snapshots (trade_date);

CREATE TABLE early_market_after_hours_snapshots (
    id BIGINT NOT NULL AUTO_INCREMENT,
    trade_date DATE NOT NULL,
    previous_trading_day DATE NOT NULL,
    captured_at TIMESTAMP(6) NOT NULL,
    stock_code VARCHAR(20) NOT NULL,
    after_hours_price DECIMAL(19, 4) NOT NULL,
    after_hours_change_rate DECIMAL(19, 4) NOT NULL,
    after_hours_volume BIGINT NOT NULL,
    after_hours_trading_value DECIMAL(19, 4) NOT NULL,
    source VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_early_market_after_hours_snapshot
        UNIQUE (trade_date, previous_trading_day, stock_code)
);

CREATE INDEX idx_early_market_after_hours_trade_date
    ON early_market_after_hours_snapshots (trade_date);

CREATE TABLE early_market_intraday_bar_snapshots (
    id BIGINT NOT NULL AUTO_INCREMENT,
    trade_date DATE NOT NULL,
    stock_code VARCHAR(20) NOT NULL,
    captured_at TIMESTAMP(6) NOT NULL,
    bar_time TIME NOT NULL,
    interval_type VARCHAR(30) NOT NULL,
    open_price DECIMAL(19, 4) NOT NULL,
    high_price DECIMAL(19, 4) NOT NULL,
    low_price DECIMAL(19, 4) NOT NULL,
    close_price DECIMAL(19, 4) NOT NULL,
    volume BIGINT NOT NULL,
    trading_value DECIMAL(19, 4) NOT NULL,
    vwap DECIMAL(19, 4) NOT NULL,
    source VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_early_market_intraday_bar
        UNIQUE (trade_date, stock_code, bar_time, interval_type)
);

CREATE INDEX idx_early_market_intraday_trade_stock
    ON early_market_intraday_bar_snapshots (trade_date, stock_code);

CREATE TABLE early_market_market_snapshots (
    id BIGINT NOT NULL AUTO_INCREMENT,
    trade_date DATE NOT NULL,
    stock_code VARCHAR(20) NOT NULL,
    captured_at TIMESTAMP(6) NOT NULL,
    snapshot_type VARCHAR(30) NOT NULL,
    current_price DECIMAL(19, 4) NOT NULL,
    day_high DECIMAL(19, 4) NOT NULL,
    day_low DECIMAL(19, 4) NOT NULL,
    accumulated_volume BIGINT NOT NULL,
    accumulated_trading_value DECIMAL(19, 4) NOT NULL,
    vwap DECIMAL(19, 4),
    source VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_early_market_market_snapshot
        UNIQUE (trade_date, stock_code, captured_at, snapshot_type)
);

CREATE INDEX idx_early_market_market_snapshot_trade_stock
    ON early_market_market_snapshots (trade_date, stock_code);
