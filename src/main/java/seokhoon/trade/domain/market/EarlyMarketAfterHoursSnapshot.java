package seokhoon.trade.domain.market;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record EarlyMarketAfterHoursSnapshot(
        Long id,
        LocalDate tradeDate,
        LocalDate previousTradingDay,
        Instant capturedAt,
        String stockCode,
        BigDecimal afterHoursPrice,
        BigDecimal afterHoursChangeRate,
        long afterHoursVolume,
        BigDecimal afterHoursTradingValue,
        String source
) {
}
