package seokhoon.trade.domain.research;

import java.time.Instant;
import java.util.Objects;

public record DartFinancialImportHistory(
        Long id,
        String stockCode,
        String corpCode,
        int fiscalYear,
        String reportCode,
        DartFinancialImportStatus status,
        int importedQuarterlyFinancialCount,
        String failureReason,
        Instant requestedAt,
        Instant completedAt
) {
    public DartFinancialImportHistory {
        Objects.requireNonNull(stockCode, "stockCode");
        Objects.requireNonNull(reportCode, "reportCode");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(requestedAt, "requestedAt");
        Objects.requireNonNull(completedAt, "completedAt");
    }
}
