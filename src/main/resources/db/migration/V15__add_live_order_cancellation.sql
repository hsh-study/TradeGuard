ALTER TABLE live_order_requests
    ADD COLUMN remaining_quantity INT NOT NULL DEFAULT 0;
ALTER TABLE live_order_requests
    ADD COLUMN filled_quantity INT NOT NULL DEFAULT 0;
ALTER TABLE live_order_requests
    ADD COLUMN last_inquired_at TIMESTAMP(6);
ALTER TABLE live_order_requests
    ADD COLUMN cancel_requested_at TIMESTAMP(6);
ALTER TABLE live_order_requests
    ADD COLUMN canceled_at TIMESTAMP(6);
ALTER TABLE live_order_requests
    ADD COLUMN expire_at TIMESTAMP(6);

UPDATE live_order_requests
SET filled_quantity = COALESCE((
    SELECT SUM(live_trade_fills.filled_quantity)
    FROM live_trade_fills
    WHERE live_trade_fills.live_order_request_id = live_order_requests.id
), 0);

UPDATE live_order_requests
SET remaining_quantity = CASE
    WHEN status IN ('FILLED', 'CANCELED') THEN 0
    ELSE quantity - filled_quantity
END;

CREATE TABLE live_order_cancel_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    live_order_request_id BIGINT NOT NULL,
    kis_original_order_no VARCHAR(100),
    cancel_quantity INT NOT NULL,
    status VARCHAR(30) NOT NULL,
    kis_cancel_order_no VARCHAR(100),
    failure_reason VARCHAR(1000),
    reason VARCHAR(1000) NOT NULL,
    requested_at TIMESTAMP(6) NOT NULL,
    submitted_at TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_live_order_cancel_request_order
        FOREIGN KEY (live_order_request_id) REFERENCES live_order_requests (id)
);

CREATE INDEX idx_live_order_cancel_order
    ON live_order_cancel_requests (live_order_request_id, requested_at);
CREATE INDEX idx_live_order_cancel_status
    ON live_order_cancel_requests (status);
