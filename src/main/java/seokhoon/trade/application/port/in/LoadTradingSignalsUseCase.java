package seokhoon.trade.application.port.in;

import java.util.List;

public interface LoadTradingSignalsUseCase {
    List<TradingSignalView> load(TradingSignalSearchCriteria criteria);
}
