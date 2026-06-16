CREATE TABLE investment_theses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    stock_code VARCHAR(20) NOT NULL,
    title VARCHAR(255) NOT NULL,
    core_assumption TEXT NOT NULL,
    invalidation_condition TEXT NOT NULL,
    target_price DECIMAL(19, 4),
    stop_loss_condition TEXT NOT NULL,
    confidence INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_investment_thesis_stock_status
    ON investment_theses (stock_code, status);

CREATE TABLE investment_catalysts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    stock_code VARCHAR(20),
    title VARCHAR(255) NOT NULL,
    catalyst_type VARCHAR(30) NOT NULL,
    expected_date DATE NOT NULL,
    importance VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    source_url VARCHAR(1000),
    memo TEXT,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_investment_catalyst_date_status
    ON investment_catalysts (expected_date, status);
CREATE INDEX idx_investment_catalyst_stock
    ON investment_catalysts (stock_code);

CREATE TABLE morning_notes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    trade_date DATE NOT NULL,
    market_summary TEXT NOT NULL,
    sector_summary TEXT NOT NULL,
    portfolio_impact_summary TEXT NOT NULL,
    watchlist_summary TEXT NOT NULL,
    action_items TEXT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_morning_note_trade_date UNIQUE (trade_date)
);
