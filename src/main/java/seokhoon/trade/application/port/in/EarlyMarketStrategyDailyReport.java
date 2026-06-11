package seokhoon.trade.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record EarlyMarketStrategyDailyReport(
        LocalDate tradeDate,
        int preScanCount,
        int entryCandidateCount,
        int performanceCapturedCount,
        int excludedFromPerformanceCount,
        BigDecimal averageMaxReturnRate,
        BigDecimal averageMaxDrawdownRate,
        EarlyMarketStrategyCandidateReport bestCandidate,
        EarlyMarketStrategyCandidateReport worstCandidate,
        Map<String, EarlyMarketStrategyGroupReport> bySignalType,
        Map<String, EarlyMarketStrategyGroupReport> byScoreBucket,
        Map<String, EarlyMarketStrategyGroupReport> byVwapBroken,
        Map<String, EarlyMarketStrategyGroupReport> byPreviousHighBreakout,
        Map<String, EarlyMarketStrategyGroupReport> byOpeningPriceHeld,
        EarlyMarketReportDataCompleteness dataCompleteness,
        List<EarlyMarketStrategyCandidateReport> candidates
) {
}
