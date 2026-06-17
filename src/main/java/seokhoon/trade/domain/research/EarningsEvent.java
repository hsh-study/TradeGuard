package seokhoon.trade.domain.research;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public record EarningsEvent(
        Long id,
        String stockCode,
        int fiscalYear,
        int fiscalQuarter,
        LocalDate expectedAnnouncementDate,
        LocalDate actualAnnouncementDate,
        EarningsEventStatus status,
        String memo,
        Instant createdAt,
        Instant updatedAt
) {
    public EarningsEvent {
        Objects.requireNonNull(stockCode, "stockCode");
        Objects.requireNonNull(expectedAnnouncementDate, "expectedAnnouncementDate");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if (fiscalQuarter < 1 || fiscalQuarter > 4) {
            throw new IllegalArgumentException("fiscalQuarter must be 1..4");
        }
    }
}
