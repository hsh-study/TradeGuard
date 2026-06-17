package seokhoon.trade.domain.market;

import java.time.Instant;
import java.util.Objects;

public record SectorImportHistory(
        Long id,
        SectorImportStatus status,
        int importedSectorCount,
        int importedMappingCount,
        String failureReason,
        Instant requestedAt,
        Instant completedAt
) {
    public SectorImportHistory {
        Objects.requireNonNull(status, "status");
        if (importedSectorCount < 0) {
            throw new IllegalArgumentException("importedSectorCount must not be negative");
        }
        if (importedMappingCount < 0) {
            throw new IllegalArgumentException("importedMappingCount must not be negative");
        }
        Objects.requireNonNull(requestedAt, "requestedAt");
        Objects.requireNonNull(completedAt, "completedAt");
    }
}
