package seokhoon.trade.domain.research;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public record DisclosureEvidenceImportHistory(
        Long id,
        DisclosureProvider provider,
        String stockCode,
        LocalDate fromDate,
        LocalDate toDate,
        DisclosureEvidenceImportStatus status,
        int importedCount,
        String failureReason,
        Instant requestedAt,
        Instant completedAt
) {
    public DisclosureEvidenceImportHistory {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(fromDate, "fromDate");
        Objects.requireNonNull(toDate, "toDate");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(requestedAt, "requestedAt");
        Objects.requireNonNull(completedAt, "completedAt");
    }
}
