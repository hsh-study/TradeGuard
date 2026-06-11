package seokhoon.trade.application.port.in;

import java.math.BigDecimal;

public record EarlyMarketStrategyDailySummary(
        int candidateCount,
        int performanceCapturedCount,
        int excludedFromPerformanceCount,
        BigDecimal averageMaxReturnRate,
        BigDecimal averageMaxDrawdownRate,
        BigDecimal winRate,
        EarlyMarketReportDataCompleteness dataCompleteness
) {
}
