package seokhoon.trade.domain.research;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ReplayBacktestResult(
        Long id,
        Long runId,
        LocalDate tradeDate,
        String stockCode,
        String stockName,
        ReplayBacktestStrategy strategy,
        int candidateRank,
        int score,
        List<String> reasons,
        List<String> warnings,
        BigDecimal entryReferencePrice,
        BigDecimal exitReferencePrice,
        Integer holdingDays,
        BigDecimal returnRate,
        ReplayBacktestResultStatus resultStatus,
        Instant createdAt
) {
}
