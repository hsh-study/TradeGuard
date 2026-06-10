package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.strategy.SignalType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record EarlyMarketPerformanceView(
        long signalId,
        String stockCode,
        LocalDate tradeDate,
        SignalType signalType,
        int signalScore,
        BigDecimal entryReferencePrice,
        BigDecimal highUntil0930,
        BigDecimal lowUntil0930,
        BigDecimal priceAt0930,
        BigDecimal maxReturnRateUntil0930,
        BigDecimal maxDrawdownRateUntil0930,
        Boolean vwapBroken,
        Instant capturedAt
) {
}
