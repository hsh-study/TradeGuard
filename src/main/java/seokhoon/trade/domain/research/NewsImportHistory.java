package seokhoon.trade.domain.research;
import java.time.Instant;
public record NewsImportHistory(Long id, String query, int requestedDisplay, int fetchedCount,
        int savedCount, int duplicatedCount, NewsImportStatus status, String failureReason,
        Instant startedAt, Instant finishedAt) {}
