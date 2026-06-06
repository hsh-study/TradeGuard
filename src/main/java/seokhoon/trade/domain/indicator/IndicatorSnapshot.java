package seokhoon.trade.domain.indicator;

import java.math.BigDecimal;
import java.time.LocalDate;

public record IndicatorSnapshot(
        String stockCode,
        LocalDate tradeDate,
        BigDecimal ma5,
        BigDecimal ma20,
        BigDecimal ma60,
        BigDecimal rsi14,
        BigDecimal macd,
        BigDecimal macdSignal,
        BigDecimal macdHistogram,
        BigDecimal bollingerUpper,
        BigDecimal bollingerMiddle,
        BigDecimal bollingerLower
) {
}
