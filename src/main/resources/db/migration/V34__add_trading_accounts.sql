CREATE TABLE trading_accounts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    alias VARCHAR(100) NOT NULL,
    environment VARCHAR(10) NOT NULL,
    encrypted_account_number VARCHAR(512) NOT NULL,
    product_code VARCHAR(2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    primary_account BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_trading_accounts_alias_environment UNIQUE (alias, environment),
    CONSTRAINT chk_trading_accounts_environment CHECK (environment IN ('REAL', 'DEMO'))
);

CREATE INDEX idx_trading_accounts_environment_active
    ON trading_accounts (environment, active, primary_account);
