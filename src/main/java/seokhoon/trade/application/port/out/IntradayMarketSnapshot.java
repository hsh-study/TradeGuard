package seokhoon.trade.application.port.out;

import java.math.BigDecimal;
import java.time.Instant;

public record IntradayMarketSnapshot(
        String stockCode,
        BigDecimal currentPrice,
        BigDecimal changeRate,
        BigDecimal intradayHigh,
        BigDecimal intradayLow,
        long accumulatedVolume,
        BigDecimal accumulatedTradingValue,
        BigDecimal vwap,
        Instant snapshotTime
) {
}
