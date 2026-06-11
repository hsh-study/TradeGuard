package seokhoon.trade.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record EarlyMarketStrategyPeriodReport(
        LocalDate from,
        LocalDate to,
        int tradingDayCount,
        int candidateCount,
        int performanceCapturedCount,
        int excludedFromPerformanceCount,
        BigDecimal averageMaxReturnRate,
        BigDecimal averageMaxDrawdownRate,
        BigDecimal winRate,
        EarlyMarketStrategyCandidateReport bestCandidate,
        EarlyMarketStrategyCandidateReport worstCandidate,
        Map<LocalDate, EarlyMarketStrategyDailySummary> byTradeDate,
        Map<String, EarlyMarketStrategyGroupReport> bySignalType,
        Map<String, EarlyMarketStrategyGroupReport> byScoreBucket,
        Map<String, EarlyMarketStrategyGroupReport> byVwapBroken,
        Map<String, EarlyMarketStrategyGroupReport> byPreviousHighBreakout,
        Map<String, EarlyMarketStrategyGroupReport> byOpeningPriceHeld,
        Map<String, EarlyMarketStrategyGroupReport> byFollowUpDecision,
        EarlyMarketReportDataCompleteness dataCompleteness
) {
}
