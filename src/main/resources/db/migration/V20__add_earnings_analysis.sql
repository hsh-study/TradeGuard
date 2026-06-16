CREATE TABLE quarterly_financials (
    id BIGINT NOT NULL AUTO_INCREMENT,
    stock_code VARCHAR(20) NOT NULL,
    fiscal_year INT NOT NULL,
    fiscal_quarter INT NOT NULL,
    revenue DECIMAL(19, 4) NOT NULL,
    operating_income DECIMAL(19, 4) NOT NULL,
    net_income DECIMAL(19, 4) NOT NULL,
    total_assets DECIMAL(19, 4) NOT NULL,
    total_liabilities DECIMAL(19, 4) NOT NULL,
    total_equity DECIMAL(19, 4) NOT NULL,
    operating_cash_flow DECIMAL(19, 4) NOT NULL,
    free_cash_flow DECIMAL(19, 4) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_quarterly_financial_stock_quarter UNIQUE (stock_code, fiscal_year, fiscal_quarter)
);

CREATE INDEX idx_quarterly_financial_stock_recent
    ON quarterly_financials (stock_code, fiscal_year, fiscal_quarter);

CREATE TABLE valuation_snapshots (
    id BIGINT NOT NULL AUTO_INCREMENT,
    stock_code VARCHAR(20) NOT NULL,
    trade_date DATE NOT NULL,
    market_cap DECIMAL(19, 4) NOT NULL,
    per DECIMAL(19, 4),
    pbr DECIMAL(19, 4),
    psr DECIMAL(19, 4),
    eps DECIMAL(19, 4),
    bps DECIMAL(19, 4),
    sales_per_share DECIMAL(19, 4),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_valuation_snapshot_stock_date UNIQUE (stock_code, trade_date)
);

CREATE INDEX idx_valuation_snapshot_stock_date
    ON valuation_snapshots (stock_code, trade_date);

CREATE TABLE earnings_analysis_snapshots (
    id BIGINT NOT NULL AUTO_INCREMENT,
    stock_code VARCHAR(20) NOT NULL,
    base_date DATE NOT NULL,
    revenue_yoy_growth DECIMAL(19, 4),
    operating_income_yoy_growth DECIMAL(19, 4),
    net_income_yoy_growth DECIMAL(19, 4),
    operating_margin DECIMAL(19, 4),
    net_margin DECIMAL(19, 4),
    debt_ratio DECIMAL(19, 4),
    operating_cash_flow DECIMAL(19, 4),
    free_cash_flow DECIMAL(19, 4),
    per DECIMAL(19, 4),
    pbr DECIMAL(19, 4),
    psr DECIMAL(19, 4),
    earnings_quality_score INT,
    valuation_score INT,
    overall_score INT,
    status VARCHAR(30) NOT NULL,
    reasons TEXT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_earnings_analysis_stock_date UNIQUE (stock_code, base_date)
);

CREATE INDEX idx_earnings_analysis_base_date
    ON earnings_analysis_snapshots (base_date, status);
