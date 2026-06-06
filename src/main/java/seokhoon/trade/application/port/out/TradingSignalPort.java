package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.strategy.TradingSignal;

public interface TradingSignalPort {
    TradingSignal save(TradingSignal tradingSignal);
}
