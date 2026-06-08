package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.strategy.SignalType;
import seokhoon.trade.domain.strategy.TradingSignal;

import java.time.LocalDate;
import java.util.Optional;

public interface TradingSignalPort {
    TradingSignal save(TradingSignal tradingSignal);

    Optional<TradingSignal> find(
            String strategyName,
            String stockCode,
            LocalDate signalDate,
            SignalType signalType
    );

    Optional<TradingSignal> findById(long signalId);
}
