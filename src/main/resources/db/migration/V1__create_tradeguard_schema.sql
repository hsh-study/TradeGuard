CREATE TABLE stocks (
    stock_code VARCHAR(255) NOT NULL,
    stock_name VARCHAR(255),
    market VARCHAR(255),
    active BOOLEAN NOT NULL,
    PRIMARY KEY (stock_code)
);

CREATE TABLE daily_prices (
    stock_code VARCHAR(255) NOT NULL,
    trade_date DATE NOT NULL,
    open_price DECIMAL(38, 4),
    high_price DECIMAL(38, 4),
    low_price DECIMAL(38, 4),
    close_price DECIMAL(38, 4),
    volume BIGINT NOT NULL,
    trading_value DECIMAL(38, 4),
    PRIMARY KEY (stock_code, trade_date)
);

CREATE TABLE indicator_snapshots (
    id BIGINT NOT NULL AUTO_INCREMENT,
    stock_code VARCHAR(255) NOT NULL,
    trade_date DATE NOT NULL,
    ma5 DECIMAL(38, 4),
    ma20 DECIMAL(38, 4),
    ma60 DECIMAL(38, 4),
    rsi14 DECIMAL(38, 4),
    macd DECIMAL(38, 4),
    macd_signal DECIMAL(38, 4),
    macd_histogram DECIMAL(38, 4),
    bollinger_upper DECIMAL(38, 4),
    bollinger_middle DECIMAL(38, 4),
    bollinger_lower DECIMAL(38, 4),
    PRIMARY KEY (id),
    CONSTRAINT uk_indicator_snapshot_stock_date UNIQUE (stock_code, trade_date)
);

CREATE TABLE trading_signals (
    id BIGINT NOT NULL AUTO_INCREMENT,
    strategy_name VARCHAR(255) NOT NULL,
    stock_code VARCHAR(255) NOT NULL,
    signal_date DATE NOT NULL,
    signal_type VARCHAR(255) NOT NULL,
    score INT NOT NULL,
    status VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT uk_trading_signal_strategy_stock_date_type
        UNIQUE (strategy_name, stock_code, signal_date, signal_type)
);

CREATE TABLE trading_signal_reasons (
    trading_signal_id BIGINT NOT NULL,
    reasons VARCHAR(255),
    CONSTRAINT fk_trading_signal_reasons_signal
        FOREIGN KEY (trading_signal_id) REFERENCES trading_signals (id)
);

CREATE TABLE trading_signal_risk_reasons (
    trading_signal_id BIGINT NOT NULL,
    risk_reasons VARCHAR(255),
    CONSTRAINT fk_trading_signal_risk_reasons_signal
        FOREIGN KEY (trading_signal_id) REFERENCES trading_signals (id)
);

CREATE TABLE order_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    stock_code VARCHAR(255) NOT NULL,
    side VARCHAR(255) NOT NULL,
    order_type VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    limit_price DECIMAL(38, 4) NOT NULL,
    status VARCHAR(255) NOT NULL,
    broker_order_no VARCHAR(255),
    strategy_name VARCHAR(255) NOT NULL,
    trade_date DATE NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_order_request_stock_strategy_date_side
        UNIQUE (stock_code, strategy_name, trade_date, side)
);
