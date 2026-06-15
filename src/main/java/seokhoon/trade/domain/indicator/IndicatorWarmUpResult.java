package seokhoon.trade.domain.indicator;

import java.time.LocalDate;
import java.util.List;

public record IndicatorWarmUpResult(
        String stockCode,
        LocalDate baseDate,
        LocalDate requestedFrom,
        LocalDate requestedTo,
        int importedDailyPriceCount,
        int totalDailyPriceCount,
        boolean indicatorCalculated,
        boolean sufficientForMa20,
        boolean sufficientForMa60,
        List<String> warnings,
        IndicatorWarmUpStatus status
) {
    public IndicatorWarmUpResult {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
