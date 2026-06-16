package seokhoon.trade.domain.market;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public record MarketIndex(
        Long id,
        String indexCode,
        String indexName,
        LocalDate tradeDate,
        BigDecimal closePrice,
        BigDecimal changeRate,
        BigDecimal tradingValue,
        Instant createdAt,
        Instant updatedAt
) {
    public MarketIndex {
        requireText(indexCode, "indexCode");
        requireText(indexName, "indexName");
        Objects.requireNonNull(tradeDate, "tradeDate");
        requireNonNegative(closePrice, "closePrice");
        Objects.requireNonNull(changeRate, "changeRate");
        requireNonNegative(tradingValue, "tradingValue");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }

    private static void requireNonNegative(BigDecimal value, String field) {
        Objects.requireNonNull(value, field);
        if (value.signum() < 0) {
            throw new IllegalArgumentException(field + " must not be negative");
        }
    }
}
