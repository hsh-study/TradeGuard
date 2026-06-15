package seokhoon.trade.domain.indicator;

import java.time.Instant;
import java.time.LocalDate;

public record IndicatorWarmUpHistory(
        Long id,
        String stockCode,
        LocalDate baseDate,
        IndicatorWarmUpStatus status,
        int importedDailyPriceCount,
        int totalDailyPriceCount,
        boolean sufficientForMa20,
        boolean sufficientForMa60,
        String failureReason,
        Instant createdAt
) {
}
