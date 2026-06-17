package seokhoon.trade.domain.research;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public record SharesOutstandingSnapshot(
        Long id,
        String stockCode,
        LocalDate baseDate,
        BigDecimal sharesOutstanding,
        SharesOutstandingSource source,
        Instant createdAt,
        Instant updatedAt
) {
    public SharesOutstandingSnapshot {
        Objects.requireNonNull(stockCode, "stockCode");
        Objects.requireNonNull(baseDate, "baseDate");
        Objects.requireNonNull(sharesOutstanding, "sharesOutstanding");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if (sharesOutstanding.signum() <= 0) {
            throw new IllegalArgumentException("sharesOutstanding must be positive");
        }
    }
}
