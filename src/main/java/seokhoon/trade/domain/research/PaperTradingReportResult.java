package seokhoon.trade.domain.research;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PaperTradingReportResult(
        Long id, Long runId, LocalDate tradeDate, PaperTradingStrategy strategy,
        String stockCode, String stockName, int candidateRank, Long signalId, int score,
        List<String> reasons, List<String> warnings, BigDecimal referenceEntryPrice,
        BigDecimal referenceExitPrice, BigDecimal highAfterEntry, BigDecimal lowAfterEntry,
        BigDecimal maxFavorableExcursion, BigDecimal maxAdverseExcursion,
        BigDecimal returnRate, PaperTradingResultStatus resultStatus, Instant createdAt
) { }
