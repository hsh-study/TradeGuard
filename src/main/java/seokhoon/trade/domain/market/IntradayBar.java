package seokhoon.trade.domain.market;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

public record IntradayBar(
        String stockCode,
        LocalDate tradeDate,
        LocalTime barTime,
        BigDecimal openPrice,
        BigDecimal highPrice,
        BigDecimal lowPrice,
        BigDecimal closePrice,
        long volume,
        BigDecimal tradingValue,
        BigDecimal vwap
) {
    public IntradayBar {
        if (stockCode == null || stockCode.isBlank()) {
            throw new IllegalArgumentException("stockCode must not be blank");
        }
        Objects.requireNonNull(tradeDate, "tradeDate");
        Objects.requireNonNull(barTime, "barTime");
        requirePositive(openPrice, "openPrice");
        requirePositive(highPrice, "highPrice");
        requirePositive(lowPrice, "lowPrice");
        requirePositive(closePrice, "closePrice");
        if (volume < 0) {
            throw new IllegalArgumentException("volume must not be negative");
        }
        requireNonNegative(tradingValue, "tradingValue");
        requirePositive(vwap, "vwap");
        if (highPrice.compareTo(lowPrice) < 0
                || highPrice.compareTo(openPrice) < 0
                || highPrice.compareTo(closePrice) < 0
                || lowPrice.compareTo(openPrice) > 0
                || lowPrice.compareTo(closePrice) > 0) {
            throw new IllegalArgumentException("OHLC prices are inconsistent");
        }
    }

    private static void requirePositive(BigDecimal value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        if (value.signum() <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
    }

    private static void requireNonNegative(BigDecimal value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        if (value.signum() < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }
    }
}
