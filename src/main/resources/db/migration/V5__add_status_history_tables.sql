CREATE TABLE trading_signal_status_histories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    trading_signal_id BIGINT NOT NULL,
    from_status VARCHAR(255) NOT NULL,
    to_status VARCHAR(255) NOT NULL,
    reason VARCHAR(1000),
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_signal_status_history_signal
        FOREIGN KEY (trading_signal_id) REFERENCES trading_signals (id)
);

CREATE INDEX idx_signal_status_history_target_created
    ON trading_signal_status_histories (trading_signal_id, created_at);

CREATE TABLE order_request_status_histories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_request_id BIGINT NOT NULL,
    from_status VARCHAR(255) NOT NULL,
    to_status VARCHAR(255) NOT NULL,
    reason VARCHAR(1000),
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_order_status_history_order
        FOREIGN KEY (order_request_id) REFERENCES order_requests (id)
);

CREATE INDEX idx_order_status_history_target_created
    ON order_request_status_histories (order_request_id, created_at);
