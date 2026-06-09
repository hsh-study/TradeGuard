ALTER TABLE order_requests
    ADD COLUMN signal_id BIGINT NULL;

CREATE INDEX idx_order_requests_signal_id
    ON order_requests (signal_id);

ALTER TABLE order_requests
    ADD CONSTRAINT fk_order_requests_trading_signal
        FOREIGN KEY (signal_id) REFERENCES trading_signals (id);
