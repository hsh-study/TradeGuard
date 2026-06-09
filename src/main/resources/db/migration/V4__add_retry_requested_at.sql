ALTER TABLE order_requests
    ADD COLUMN retry_requested_at TIMESTAMP(6) NULL;

CREATE INDEX idx_order_requests_status_retry_requested_at
    ON order_requests (status, retry_requested_at);
