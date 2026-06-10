package seokhoon.trade.application.port.in;

import java.time.LocalDate;
import java.util.List;

public interface LoadEarlyMarketPerformancesUseCase {
    List<EarlyMarketPerformanceView> findByTradeDate(LocalDate tradeDate);

    EarlyMarketPerformanceView findBySignalId(long signalId);
}
