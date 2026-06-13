package seokhoon.trade.domain.market;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record EarlyMarketIntradayBarSnapshot(
        Long id,
        LocalDate tradeDate,
        String stockCode,
        Instant capturedAt,
        LocalTime barTime,
        BarInterval intervalType,
        BigDecimal openPrice,
        BigDecimal highPrice,
        BigDecimal lowPrice,
        BigDecimal closePrice,
        long volume,
        BigDecimal tradingValue,
        BigDecimal vwap,
        String source
) {
}
