package seokhoon.trade.domain.research;

import java.time.Instant;
import java.util.Objects;

public record SharesOutstandingImportHistory(
        Long id,
        SharesOutstandingImportStatus status,
        int importedCount,
        String failureReason,
        Instant requestedAt,
        Instant completedAt
) {
    public SharesOutstandingImportHistory {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(requestedAt, "requestedAt");
        Objects.requireNonNull(completedAt, "completedAt");
    }
}
