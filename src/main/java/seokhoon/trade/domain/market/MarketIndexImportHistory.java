package seokhoon.trade.domain.market;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public record MarketIndexImportHistory(
        Long id,
        MarketIndexImportProvider provider,
        LocalDate tradeDate,
        MarketIndexImportStatus status,
        int importedCount,
        String failureReason,
        Instant requestedAt,
        Instant completedAt
) {
    public MarketIndexImportHistory {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(tradeDate, "tradeDate");
        Objects.requireNonNull(status, "status");
        if (importedCount < 0) {
            throw new IllegalArgumentException("importedCount must not be negative");
        }
        Objects.requireNonNull(requestedAt, "requestedAt");
        Objects.requireNonNull(completedAt, "completedAt");
    }
}
