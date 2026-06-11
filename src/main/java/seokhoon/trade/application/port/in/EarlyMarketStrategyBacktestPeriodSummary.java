package seokhoon.trade.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EarlyMarketStrategyBacktestPeriodSummary(
        LocalDate from,
        LocalDate to,
        int tradingDayCount,
        int candidateCount,
        int performanceCapturedCount,
        int excludedFromPerformanceCount,
        BigDecimal averageMaxReturnRate,
        BigDecimal averageMaxDrawdownRate,
        BigDecimal winRate,
        Long bestSignalId,
        Long worstSignalId,
        EarlyMarketReportDataCompleteness dataCompleteness
) {
}
