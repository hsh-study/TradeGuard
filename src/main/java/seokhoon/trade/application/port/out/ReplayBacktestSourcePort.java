package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.market.DailyPrice;
import seokhoon.trade.domain.market.EarlyMarketIntradayBarSnapshot;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReplayBacktestSourcePort {
    Optional<String> findStockName(String stockCode);
    Optional<DailyPrice> findDailyPrice(String stockCode, LocalDate tradeDate);
    Optional<DailyPrice> findNthDailyPriceAfter(String stockCode, LocalDate tradeDate, int tradingDays);
    List<EarlyMarketIntradayBarSnapshot> findIntradayBars(String stockCode, LocalDate tradeDate);
}
