CREATE TABLE paper_trading_report_runs (
    id BIGINT NOT NULL AUTO_INCREMENT, trade_date DATE NOT NULL, status VARCHAR(20) NOT NULL,
    total_candidates INT NOT NULL, average_return_rate DECIMAL(19,6) NULL,
    win_count INT NOT NULL, loss_count INT NOT NULL, flat_count INT NOT NULL,
    failure_reason VARCHAR(1000) NULL, created_at TIMESTAMP(6) NOT NULL, completed_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id)
);

CREATE TABLE paper_trading_report_results (
    id BIGINT NOT NULL AUTO_INCREMENT, run_id BIGINT NOT NULL, trade_date DATE NOT NULL,
    strategy VARCHAR(30) NOT NULL, stock_code VARCHAR(20) NOT NULL, stock_name VARCHAR(100) NOT NULL,
    candidate_rank INT NOT NULL, signal_id BIGINT NULL, score INT NOT NULL,
    reasons TEXT NOT NULL, warnings TEXT NOT NULL,
    reference_entry_price DECIMAL(19,4) NULL, reference_exit_price DECIMAL(19,4) NULL,
    high_after_entry DECIMAL(19,4) NULL, low_after_entry DECIMAL(19,4) NULL,
    max_favorable_excursion DECIMAL(19,6) NULL, max_adverse_excursion DECIMAL(19,6) NULL,
    return_rate DECIMAL(19,6) NULL, result_status VARCHAR(30) NOT NULL, created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_paper_trading_report_result_run FOREIGN KEY (run_id) REFERENCES paper_trading_report_runs (id)
);

CREATE INDEX idx_paper_trading_report_run_date ON paper_trading_report_runs (trade_date, created_at);
CREATE INDEX idx_paper_trading_report_result_run ON paper_trading_report_results (run_id, strategy, candidate_rank);
