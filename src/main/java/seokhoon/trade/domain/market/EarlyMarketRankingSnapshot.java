package seokhoon.trade.domain.market;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record EarlyMarketRankingSnapshot(
        Long id,
        LocalDate tradeDate,
        Instant capturedAt,
        int rank,
        String stockCode,
        String stockName,
        BigDecimal currentPrice,
        BigDecimal changeRate,
        long volume,
        BigDecimal tradingValue,
        String source
) {
}
