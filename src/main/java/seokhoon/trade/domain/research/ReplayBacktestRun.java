package seokhoon.trade.domain.research;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ReplayBacktestRun(
        Long id,
        ReplayBacktestStrategy strategy,
        LocalDate fromDate,
        LocalDate toDate,
        ReplayBacktestStatus status,
        String parameterSnapshot,
        int totalCandidates,
        int winCount,
        int lossCount,
        BigDecimal averageReturnRate,
        BigDecimal maxReturnRate,
        BigDecimal minReturnRate,
        String failureReason,
        Instant createdAt,
        Instant completedAt
) {
}
