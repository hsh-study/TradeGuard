package seokhoon.trade.domain.strategy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

public record EarlyMarketStrategyExperiment(
        Long id,
        String experimentName,
        LocalDate from,
        LocalDate to,
        Map<String, Object> parameterSnapshot,
        int candidateCount,
        int performanceCapturedCount,
        BigDecimal averageMaxReturnRate,
        BigDecimal averageMaxDrawdownRate,
        BigDecimal winRate,
        Long bestSignalId,
        Long worstSignalId,
        Instant createdAt
) {
    public EarlyMarketStrategyExperiment {
        parameterSnapshot = Map.copyOf(parameterSnapshot);
    }
}
