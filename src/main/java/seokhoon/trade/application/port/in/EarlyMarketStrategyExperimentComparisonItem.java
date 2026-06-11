package seokhoon.trade.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record EarlyMarketStrategyExperimentComparisonItem(
        long id,
        String experimentName,
        LocalDate from,
        LocalDate to,
        int candidateCount,
        int performanceCapturedCount,
        BigDecimal averageMaxReturnRate,
        BigDecimal averageMaxDrawdownRate,
        BigDecimal winRate,
        Long bestSignalId,
        Long worstSignalId,
        Map<String, Object> parameterSnapshot
) {
}
