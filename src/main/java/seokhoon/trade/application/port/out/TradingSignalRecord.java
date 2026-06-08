package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.strategy.SignalType;
import seokhoon.trade.domain.strategy.TradingSignalStatus;

import java.time.LocalDate;
import java.util.List;

public record TradingSignalRecord(
        Long id,
        String strategyName,
        String stockCode,
        LocalDate signalDate,
        SignalType signalType,
        int score,
        List<String> reasons,
        List<String> riskReasons,
        TradingSignalStatus status
) {
}
