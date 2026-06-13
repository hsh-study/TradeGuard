package seokhoon.trade.domain.market;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record EarlyMarketMarketSnapshot(
        Long id,
        LocalDate tradeDate,
        String stockCode,
        Instant capturedAt,
        EarlyMarketSnapshotType snapshotType,
        BigDecimal currentPrice,
        BigDecimal dayHigh,
        BigDecimal dayLow,
        long accumulatedVolume,
        BigDecimal accumulatedTradingValue,
        BigDecimal vwap,
        String source
) {
}
