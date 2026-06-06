package seokhoon.trade.domain.market;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public class DailyPrice {
    private final String stockCode;
    private final LocalDate tradeDate;
    private final BigDecimal openPrice;
    private final BigDecimal highPrice;
    private final BigDecimal lowPrice;
    private final BigDecimal closePrice;
    private final long volume;
    private final BigDecimal tradingValue;

    public DailyPrice(String stockCode, LocalDate tradeDate, BigDecimal openPrice, BigDecimal highPrice,
                      BigDecimal lowPrice, BigDecimal closePrice, long volume, BigDecimal tradingValue) {
        if (stockCode == null || stockCode.isBlank()) {
            throw new IllegalArgumentException("stockCode must not be blank");
        }
        if (volume < 0) {
            throw new IllegalArgumentException("volume must not be negative");
        }
        this.stockCode = stockCode;
        this.tradeDate = Objects.requireNonNull(tradeDate, "tradeDate");
        this.openPrice = requireNonNegative(openPrice, "openPrice");
        this.highPrice = requireNonNegative(highPrice, "highPrice");
        this.lowPrice = requireNonNegative(lowPrice, "lowPrice");
        this.closePrice = requireNonNegative(closePrice, "closePrice");
        this.volume = volume;
        this.tradingValue = requireNonNegative(tradingValue, "tradingValue");
    }

    public String stockCode() { return stockCode; }
    public LocalDate tradeDate() { return tradeDate; }
    public BigDecimal openPrice() { return openPrice; }
    public BigDecimal highPrice() { return highPrice; }
    public BigDecimal lowPrice() { return lowPrice; }
    public BigDecimal closePrice() { return closePrice; }
    public long volume() { return volume; }
    public BigDecimal tradingValue() { return tradingValue; }

    private static BigDecimal requireNonNegative(BigDecimal value, String field) {
        Objects.requireNonNull(value, field);
        if (value.signum() < 0) {
            throw new IllegalArgumentException(field + " must not be negative");
        }
        return value;
    }
}
