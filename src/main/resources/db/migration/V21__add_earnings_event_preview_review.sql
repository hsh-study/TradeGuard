CREATE TABLE earnings_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    stock_code VARCHAR(20) NOT NULL,
    fiscal_year INT NOT NULL,
    fiscal_quarter INT NOT NULL,
    expected_announcement_date DATE NOT NULL,
    actual_announcement_date DATE,
    status VARCHAR(30) NOT NULL,
    memo TEXT,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_earnings_event_stock_quarter UNIQUE (stock_code, fiscal_year, fiscal_quarter)
);

CREATE INDEX idx_earnings_event_expected_date
    ON earnings_events (expected_announcement_date, status);
CREATE INDEX idx_earnings_event_stock
    ON earnings_events (stock_code);

CREATE TABLE earnings_previews (
    id BIGINT NOT NULL AUTO_INCREMENT,
    earnings_event_id BIGINT NOT NULL,
    stock_code VARCHAR(20) NOT NULL,
    preview_date DATE NOT NULL,
    key_checkpoints TEXT NOT NULL,
    expected_revenue DECIMAL(19, 4),
    expected_operating_income DECIMAL(19, 4),
    expected_net_income DECIMAL(19, 4),
    expected_operating_margin DECIMAL(19, 4),
    expected_risks TEXT NOT NULL,
    thesis_watch_points TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_earnings_preview_stock
    ON earnings_previews (stock_code, preview_date);
CREATE INDEX idx_earnings_preview_status_date
    ON earnings_previews (status, preview_date);

CREATE TABLE post_earnings_reviews (
    id BIGINT NOT NULL AUTO_INCREMENT,
    earnings_event_id BIGINT NOT NULL,
    stock_code VARCHAR(20) NOT NULL,
    review_date DATE NOT NULL,
    actual_revenue DECIMAL(19, 4) NOT NULL,
    actual_operating_income DECIMAL(19, 4) NOT NULL,
    actual_net_income DECIMAL(19, 4) NOT NULL,
    actual_operating_margin DECIMAL(19, 4),
    revenue_surprise_rate DECIMAL(19, 4),
    operating_income_surprise_rate DECIMAL(19, 4),
    thesis_impact VARCHAR(30) NOT NULL,
    review_summary TEXT NOT NULL,
    action_items TEXT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_post_earnings_review_event UNIQUE (earnings_event_id)
);

CREATE INDEX idx_post_earnings_review_stock
    ON post_earnings_reviews (stock_code, review_date);
CREATE INDEX idx_post_earnings_review_impact
    ON post_earnings_reviews (thesis_impact, review_date);
