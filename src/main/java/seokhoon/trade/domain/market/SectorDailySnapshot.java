package seokhoon.trade.domain.market;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public record SectorDailySnapshot(
        Long id,
        String sectorCode,
        LocalDate tradeDate,
        BigDecimal averageChangeRate,
        BigDecimal medianChangeRate,
        BigDecimal totalTradingValue,
        int risingStockCount,
        int fallingStockCount,
        String leadingStockCode,
        BigDecimal leadingStockChangeRate,
        Instant createdAt,
        Instant updatedAt
) {
    public SectorDailySnapshot {
        requireText(sectorCode, "sectorCode");
        Objects.requireNonNull(tradeDate, "tradeDate");
        Objects.requireNonNull(averageChangeRate, "averageChangeRate");
        Objects.requireNonNull(medianChangeRate, "medianChangeRate");
        requireNonNegative(totalTradingValue, "totalTradingValue");
        if (risingStockCount < 0 || fallingStockCount < 0) {
            throw new IllegalArgumentException("stock counts must not be negative");
        }
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public boolean dataInsufficient() {
        return leadingStockCode == null || leadingStockChangeRate == null
                || risingStockCount + fallingStockCount == 0;
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
