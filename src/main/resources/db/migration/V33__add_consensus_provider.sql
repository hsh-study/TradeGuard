CREATE TABLE earnings_consensus_snapshots (
 id BIGINT NOT NULL AUTO_INCREMENT, stock_code VARCHAR(20) NOT NULL, fiscal_year INT NOT NULL,
 fiscal_quarter INT NOT NULL, consensus_date DATE NOT NULL, expected_revenue DECIMAL(19,4),
 expected_operating_income DECIMAL(19,4), expected_net_income DECIMAL(19,4), expected_operating_margin DECIMAL(19,4),
 analyst_count INT, source VARCHAR(20) NOT NULL, provider_name VARCHAR(100), status VARCHAR(20) NOT NULL,
 created_at TIMESTAMP(6) NOT NULL, updated_at TIMESTAMP(6) NOT NULL, PRIMARY KEY(id),
 CONSTRAINT uk_earnings_consensus UNIQUE(stock_code,fiscal_year,fiscal_quarter,consensus_date,source));
CREATE INDEX idx_earnings_consensus_stock_quarter ON earnings_consensus_snapshots(stock_code,fiscal_year,fiscal_quarter,consensus_date);
CREATE TABLE target_price_consensus_snapshots (
 id BIGINT NOT NULL AUTO_INCREMENT, stock_code VARCHAR(20) NOT NULL, consensus_date DATE NOT NULL,
 target_price DECIMAL(19,4) NOT NULL, current_price DECIMAL(19,4), upside_rate DECIMAL(19,4), analyst_count INT,
 source VARCHAR(20) NOT NULL, provider_name VARCHAR(100), status VARCHAR(20) NOT NULL,
 created_at TIMESTAMP(6) NOT NULL, updated_at TIMESTAMP(6) NOT NULL, PRIMARY KEY(id),
 CONSTRAINT uk_target_price_consensus UNIQUE(stock_code,consensus_date,source));
CREATE INDEX idx_target_consensus_stock_date ON target_price_consensus_snapshots(stock_code,consensus_date);
ALTER TABLE post_earnings_reviews ADD COLUMN net_income_surprise_rate DECIMAL(19,4);
ALTER TABLE post_earnings_reviews ADD COLUMN consensus_used BOOLEAN NOT NULL DEFAULT FALSE;
