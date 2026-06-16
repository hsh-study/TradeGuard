package seokhoon.trade.domain.market;

import java.time.Instant;
import java.util.Objects;

public record StockSectorMapping(
        Long id,
        String stockCode,
        String sectorCode,
        String source,
        Instant createdAt,
        Instant updatedAt
) {
    public StockSectorMapping {
        requireText(stockCode, "stockCode");
        requireText(sectorCode, "sectorCode");
        requireText(source, "source");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
