package seokhoon.trade.domain.stock;

import java.util.Objects;

public class Stock {
    private final String stockCode;
    private final String stockName;
    private final Market market;
    private final boolean active;

    public Stock(String stockCode, String stockName, Market market, boolean active) {
        this.stockCode = requireText(stockCode, "stockCode");
        this.stockName = requireText(stockName, "stockName");
        this.market = Objects.requireNonNull(market, "market");
        this.active = active;
    }

    public String stockCode() { return stockCode; }
    public String stockName() { return stockName; }
    public Market market() { return market; }
    public boolean active() { return active; }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
