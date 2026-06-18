ALTER TABLE stock_investor_flows
    MODIFY COLUMN net_buy_amount DECIMAL(19, 4);

ALTER TABLE stock_investor_flows
    MODIFY COLUMN net_buy_quantity BIGINT;

ALTER TABLE market_investor_flows
    MODIFY COLUMN net_buy_amount DECIMAL(19, 4);
