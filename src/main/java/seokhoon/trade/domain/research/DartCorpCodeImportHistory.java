package seokhoon.trade.domain.research;

import java.time.Instant;
import java.util.Objects;

public record DartCorpCodeImportHistory(
        Long id,
        DartCorpCodeImportStatus status,
        int importedCount,
        int matchedStockCount,
        String failureReason,
        Instant requestedAt,
        Instant completedAt
) {
    public DartCorpCodeImportHistory {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(requestedAt, "requestedAt");
        Objects.requireNonNull(completedAt, "completedAt");
    }
}
