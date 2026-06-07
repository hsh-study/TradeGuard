package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.indicator.IndicatorSnapshot;
import seokhoon.trade.domain.strategy.TradingSignal;

public record AnalysisResult(
        IndicatorSnapshot indicatorSnapshot,
        TradingSignal tradingSignal
) {
}
