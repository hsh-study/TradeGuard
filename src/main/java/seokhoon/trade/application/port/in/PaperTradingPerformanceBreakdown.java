package seokhoon.trade.application.port.in;

import java.math.BigDecimal;

public record PaperTradingPerformanceBreakdown(String key, int candidateCount, int evaluatedCount,
                                               BigDecimal winRate, BigDecimal averageReturnRate) { }
