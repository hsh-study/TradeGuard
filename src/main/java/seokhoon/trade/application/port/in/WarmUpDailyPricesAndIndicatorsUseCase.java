package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.indicator.IndicatorWarmUpResult;

import java.time.LocalDate;
import java.util.List;

public interface WarmUpDailyPricesAndIndicatorsUseCase {
    IndicatorWarmUpResult warmUpStock(String stockCode, LocalDate baseDate);

    List<IndicatorWarmUpResult> warmUpStocks(
            List<String> stockCodes,
            LocalDate baseDate
    );

    static WarmUpDailyPricesAndIndicatorsUseCase noop() {
        return new WarmUpDailyPricesAndIndicatorsUseCase() {
            @Override
            public IndicatorWarmUpResult warmUpStock(
                    String stockCode,
                    LocalDate baseDate
            ) {
                return new IndicatorWarmUpResult(
                        stockCode, baseDate, null, null, 0, 0,
                        false, false, false,
                        List.of("INDICATOR_WARMUP_DISABLED"),
                        seokhoon.trade.domain.indicator
                                .IndicatorWarmUpStatus.SKIPPED
                );
            }

            @Override
            public List<IndicatorWarmUpResult> warmUpStocks(
                    List<String> stockCodes,
                    LocalDate baseDate
            ) {
                return stockCodes.stream()
                        .map(code -> warmUpStock(code, baseDate))
                        .toList();
            }
        };
    }
}
