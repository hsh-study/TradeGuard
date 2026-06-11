CREATE TABLE early_market_strategy_experiments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    experiment_name VARCHAR(100) NOT NULL,
    from_date DATE NOT NULL,
    to_date DATE NOT NULL,
    parameter_snapshot_json TEXT NOT NULL,
    candidate_count INT NOT NULL,
    performance_captured_count INT NOT NULL,
    average_max_return_rate DECIMAL(19, 4),
    average_max_drawdown_rate DECIMAL(19, 4),
    win_rate DECIMAL(19, 4),
    best_signal_id BIGINT,
    worst_signal_id BIGINT,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_early_market_experiment_created_at
    ON early_market_strategy_experiments (created_at);
