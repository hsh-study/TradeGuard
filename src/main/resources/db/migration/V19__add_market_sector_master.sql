CREATE TABLE market_indices (
    id BIGINT NOT NULL AUTO_INCREMENT,
    index_code VARCHAR(30) NOT NULL,
    index_name VARCHAR(100) NOT NULL,
    trade_date DATE NOT NULL,
    close_price DECIMAL(19, 4) NOT NULL,
    change_rate DECIMAL(10, 4) NOT NULL,
    trading_value DECIMAL(19, 4) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_market_index_code_date UNIQUE (index_code, trade_date)
);

CREATE INDEX idx_market_indices_trade_date
    ON market_indices (trade_date);

CREATE TABLE sectors (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sector_code VARCHAR(50) NOT NULL,
    sector_name VARCHAR(100) NOT NULL,
    sector_type VARCHAR(30) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_sector_code UNIQUE (sector_code)
);

CREATE TABLE stock_sector_mappings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    stock_code VARCHAR(20) NOT NULL,
    sector_code VARCHAR(50) NOT NULL,
    source VARCHAR(100) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_stock_sector_mapping UNIQUE (stock_code, sector_code)
);

CREATE INDEX idx_stock_sector_mapping_sector
    ON stock_sector_mappings (sector_code);

CREATE TABLE sector_daily_snapshots (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sector_code VARCHAR(50) NOT NULL,
    trade_date DATE NOT NULL,
    average_change_rate DECIMAL(10, 4) NOT NULL,
    median_change_rate DECIMAL(10, 4) NOT NULL,
    total_trading_value DECIMAL(19, 4) NOT NULL,
    rising_stock_count INT NOT NULL,
    falling_stock_count INT NOT NULL,
    leading_stock_code VARCHAR(20),
    leading_stock_change_rate DECIMAL(10, 4),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_sector_snapshot_code_date UNIQUE (sector_code, trade_date)
);

CREATE INDEX idx_sector_daily_snapshot_date
    ON sector_daily_snapshots (trade_date, average_change_rate);
