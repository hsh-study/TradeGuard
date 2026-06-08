ALTER TABLE order_requests
    ADD COLUMN failure_reason VARCHAR(1000) NULL;

ALTER TABLE order_requests
    ADD COLUMN failed_at TIMESTAMP(6) NULL;

ALTER TABLE order_requests
    ADD COLUMN retryable BOOLEAN NOT NULL DEFAULT FALSE;
