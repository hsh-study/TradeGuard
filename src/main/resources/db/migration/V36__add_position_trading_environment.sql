ALTER TABLE live_positions
    ADD COLUMN trading_environment VARCHAR(10) NULL AFTER stock_code;

UPDATE live_positions
SET trading_environment = (
    SELECT environment FROM trading_accounts
    WHERE active = TRUE AND primary_account = TRUE
    ORDER BY id LIMIT 1
)
WHERE trading_environment IS NULL;

CREATE INDEX idx_live_position_environment_status
    ON live_positions (trading_environment, status);
