package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.strategy.SignalType;
import seokhoon.trade.domain.strategy.TradingSignalStatus;

import java.time.LocalDate;

public record TradingSignalSearchCriteria(
        String stockCode,
        LocalDate signalDate,
        String strategyName,
        SignalType signalType,
        TradingSignalStatus status,
        Integer minScore
) {
}
