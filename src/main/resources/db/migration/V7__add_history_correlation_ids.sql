ALTER TABLE trading_signal_status_histories
    ADD COLUMN actor VARCHAR(32) NOT NULL DEFAULT 'SYSTEM';

ALTER TABLE trading_signal_status_histories
    ADD COLUMN request_correlation_id VARCHAR(128);

ALTER TABLE order_request_status_histories
    ADD COLUMN actor VARCHAR(32) NOT NULL DEFAULT 'SYSTEM';

ALTER TABLE order_request_status_histories
    ADD COLUMN request_correlation_id VARCHAR(128);

ALTER TABLE scheduler_execution_histories
    ADD COLUMN correlation_id VARCHAR(128);
