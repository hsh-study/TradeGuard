CREATE TABLE stock_investor_flows (
    id BIGINT NOT NULL AUTO_INCREMENT,
    stock_code VARCHAR(20) NOT NULL,
    trade_date DATE NOT NULL,
    investor_type VARCHAR(40) NOT NULL,
    raw_investor_type VARCHAR(100) NULL,
    net_buy_amount DECIMAL(19, 4) NOT NULL,
    net_buy_quantity BIGINT NOT NULL,
    buy_amount DECIMAL(19, 4) NULL,
    sell_amount DECIMAL(19, 4) NULL,
    buy_quantity BIGINT NULL,
    sell_quantity BIGINT NULL,
    source VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_stock_investor_flow UNIQUE (stock_code, trade_date, investor_type, source)
);

CREATE TABLE market_investor_flows (
    id BIGINT NOT NULL AUTO_INCREMENT,
    market VARCHAR(20) NOT NULL,
    trade_date DATE NOT NULL,
    investor_type VARCHAR(40) NOT NULL,
    raw_investor_type VARCHAR(100) NULL,
    net_buy_amount DECIMAL(19, 4) NOT NULL,
    net_buy_quantity BIGINT NULL,
    buy_amount DECIMAL(19, 4) NULL,
    sell_amount DECIMAL(19, 4) NULL,
    source VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_market_investor_flow UNIQUE (market, trade_date, investor_type, source)
);

CREATE TABLE stock_supply_demand_snapshots (
    id BIGINT NOT NULL AUTO_INCREMENT,
    stock_code VARCHAR(20) NOT NULL,
    trade_date DATE NOT NULL,
    foreign_net_buy_amount DECIMAL(19, 4) NOT NULL,
    institution_net_buy_amount DECIMAL(19, 4) NOT NULL,
    individual_net_buy_amount DECIMAL(19, 4) NOT NULL,
    consecutive_foreign_buy_days INT NOT NULL,
    consecutive_institution_buy_days INT NOT NULL,
    consecutive_combined_smart_money_buy_days INT NOT NULL,
    smart_money_net_buy_amount DECIMAL(19, 4) NOT NULL,
    smart_money_5day_net_buy_amount DECIMAL(19, 4) NOT NULL,
    individual_dominance_ratio DECIMAL(10, 6) NOT NULL,
    supply_demand_score INT NOT NULL,
    status VARCHAR(40) NOT NULL,
    reasons TEXT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_stock_supply_demand UNIQUE (stock_code, trade_date)
);

CREATE TABLE investor_flow_import_histories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    scope VARCHAR(20) NOT NULL,
    stock_code VARCHAR(20) NULL,
    market VARCHAR(20) NULL,
    trade_date DATE NOT NULL,
    provider VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    imported_count INT NOT NULL,
    failure_reason VARCHAR(1000) NULL,
    requested_at TIMESTAMP(6) NOT NULL,
    completed_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_stock_investor_flow_lookup
    ON stock_investor_flows (stock_code, trade_date);
CREATE INDEX idx_market_investor_flow_lookup
    ON market_investor_flows (market, trade_date);
CREATE INDEX idx_supply_demand_trade_date
    ON stock_supply_demand_snapshots (trade_date);
CREATE INDEX idx_investor_flow_history_requested
    ON investor_flow_import_histories (requested_at);
