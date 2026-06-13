CREATE TABLE live_order_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    signal_id BIGINT,
    stock_code VARCHAR(20) NOT NULL,
    side VARCHAR(10) NOT NULL,
    quantity INT NOT NULL,
    order_price DECIMAL(19, 4) NOT NULL,
    order_type VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    kis_order_no VARCHAR(100),
    kis_original_order_no VARCHAR(100),
    failure_reason VARCHAR(1000),
    requested_at TIMESTAMP(6) NOT NULL,
    submitted_at TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_live_order_buy_signal
    ON live_order_requests (signal_id, side);
CREATE INDEX idx_live_order_status
    ON live_order_requests (status);
CREATE INDEX idx_live_order_stock
    ON live_order_requests (stock_code);

CREATE TABLE live_positions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    stock_code VARCHAR(20) NOT NULL,
    quantity INT NOT NULL,
    average_buy_price DECIMAL(19, 4) NOT NULL,
    buy_amount DECIMAL(19, 4) NOT NULL,
    buy_commission DECIMAL(19, 4) NOT NULL,
    status VARCHAR(20) NOT NULL,
    opened_at TIMESTAMP(6) NOT NULL,
    closed_at TIMESTAMP(6),
    PRIMARY KEY (id)
);

CREATE INDEX idx_live_position_status
    ON live_positions (status);
CREATE INDEX idx_live_position_stock
    ON live_positions (stock_code);

CREATE TABLE live_position_exit_rules (
    id BIGINT NOT NULL AUTO_INCREMENT,
    position_id BIGINT NOT NULL,
    take_profit_rate DECIMAL(9, 4) NOT NULL,
    stop_loss_rate DECIMAL(9, 4) NOT NULL,
    max_loss_amount DECIMAL(19, 4) NOT NULL,
    sell_tax_rate DECIMAL(9, 6) NOT NULL,
    buy_commission_rate DECIMAL(9, 6) NOT NULL,
    sell_commission_rate DECIMAL(9, 6) NOT NULL,
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_live_position_exit_rule UNIQUE (position_id),
    CONSTRAINT fk_live_position_exit_rule_position
        FOREIGN KEY (position_id) REFERENCES live_positions (id)
);

CREATE TABLE live_trade_fills (
    id BIGINT NOT NULL AUTO_INCREMENT,
    live_order_request_id BIGINT NOT NULL,
    stock_code VARCHAR(20) NOT NULL,
    side VARCHAR(10) NOT NULL,
    filled_quantity INT NOT NULL,
    filled_price DECIMAL(19, 4) NOT NULL,
    filled_amount DECIMAL(19, 4) NOT NULL,
    fee DECIMAL(19, 4) NOT NULL,
    tax DECIMAL(19, 4) NOT NULL,
    filled_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_live_trade_fill_order
        FOREIGN KEY (live_order_request_id) REFERENCES live_order_requests (id)
);

CREATE INDEX idx_live_trade_fill_order
    ON live_trade_fills (live_order_request_id);

CREATE TABLE live_order_status_histories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    live_order_request_id BIGINT NOT NULL,
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    reason VARCHAR(1000),
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_live_order_status_history_order
        FOREIGN KEY (live_order_request_id) REFERENCES live_order_requests (id)
);

CREATE INDEX idx_live_order_history_order
    ON live_order_status_histories (live_order_request_id, created_at);

CREATE TABLE live_trading_runtime_state (
    id BIGINT NOT NULL,
    kill_switch_enabled BOOLEAN NOT NULL,
    reason VARCHAR(1000),
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id)
);

INSERT INTO live_trading_runtime_state (
    id, kill_switch_enabled, reason, updated_at
) VALUES (1, FALSE, NULL, CURRENT_TIMESTAMP(6));
