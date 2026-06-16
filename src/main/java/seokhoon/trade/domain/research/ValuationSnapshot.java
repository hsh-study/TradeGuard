package seokhoon.trade.domain.research;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public record ValuationSnapshot(
        Long id,
        String stockCode,
        LocalDate tradeDate,
        BigDecimal marketCap,
        BigDecimal per,
        BigDecimal pbr,
        BigDecimal psr,
        BigDecimal eps,
        BigDecimal bps,
        BigDecimal salesPerShare,
        Instant createdAt,
        Instant updatedAt
) {
    public ValuationSnapshot {
        Objects.requireNonNull(stockCode, "stockCode");
        Objects.requireNonNull(tradeDate, "tradeDate");
        Objects.requireNonNull(marketCap, "marketCap");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }
}
