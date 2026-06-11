package seokhoon.trade.application.port.in;

import java.math.BigDecimal;

public record EarlyMarketStrategyGroupReport(
        int candidateCount,
        int performanceCapturedCount,
        BigDecimal averageMaxReturnRate,
        BigDecimal averageMaxDrawdownRate
) {
}
