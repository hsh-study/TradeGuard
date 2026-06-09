package seokhoon.trade.domain.market;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record AfterHoursQuote(
        String stockCode,
        String stockName,
        LocalDate tradeDate,
        BigDecimal afterHoursPrice,
        BigDecimal afterHoursChangeRate,
        long afterHoursVolume,
        BigDecimal afterHoursTradingValue,
        Instant capturedAt
) {
}
