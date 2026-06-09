CREATE TABLE scheduler_execution_histories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    scheduler_name VARCHAR(255) NOT NULL,
    trade_date DATE NOT NULL,
    status VARCHAR(255) NOT NULL,
    skip_reason VARCHAR(1000),
    failure_reason VARCHAR(1000),
    scanned_count INT,
    selected_count INT,
    notification_sent BOOLEAN,
    started_at TIMESTAMP(6) NOT NULL,
    finished_at TIMESTAMP(6),
    PRIMARY KEY (id)
);

CREATE INDEX idx_scheduler_execution_trade_date
    ON scheduler_execution_histories (trade_date);

CREATE INDEX idx_scheduler_execution_name_trade_date
    ON scheduler_execution_histories (scheduler_name, trade_date);

CREATE INDEX idx_scheduler_execution_status
    ON scheduler_execution_histories (status);
