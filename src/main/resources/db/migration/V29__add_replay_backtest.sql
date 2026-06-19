CREATE TABLE replay_backtest_runs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    strategy VARCHAR(30) NOT NULL,
    from_date DATE NOT NULL,
    to_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    parameter_snapshot TEXT NOT NULL,
    total_candidates INT NOT NULL,
    win_count INT NOT NULL,
    loss_count INT NOT NULL,
    average_return_rate DECIMAL(19, 6) NULL,
    max_return_rate DECIMAL(19, 6) NULL,
    min_return_rate DECIMAL(19, 6) NULL,
    failure_reason VARCHAR(1000) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    completed_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id)
);

CREATE TABLE replay_backtest_results (
    id BIGINT NOT NULL AUTO_INCREMENT,
    run_id BIGINT NOT NULL,
    trade_date DATE NOT NULL,
    stock_code VARCHAR(20) NOT NULL,
    stock_name VARCHAR(100) NOT NULL,
    strategy VARCHAR(30) NOT NULL,
    candidate_rank INT NOT NULL,
    score INT NOT NULL,
    reasons TEXT NOT NULL,
    warnings TEXT NOT NULL,
    entry_reference_price DECIMAL(19, 4) NULL,
    exit_reference_price DECIMAL(19, 4) NULL,
    holding_days INT NULL,
    return_rate DECIMAL(19, 6) NULL,
    result_status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_replay_backtest_result_run FOREIGN KEY (run_id) REFERENCES replay_backtest_runs (id)
);

CREATE INDEX idx_replay_backtest_run_period ON replay_backtest_runs (strategy, from_date, to_date);
CREATE INDEX idx_replay_backtest_result_run ON replay_backtest_results (run_id, trade_date, candidate_rank);
