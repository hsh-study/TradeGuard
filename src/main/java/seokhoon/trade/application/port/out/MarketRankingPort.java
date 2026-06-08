package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.stock.Market;

import java.util.List;

public interface MarketRankingPort {
    List<MarketRankingStock> findTopTradingValueStocks(Market market, int limit);

    List<MarketRankingStock> findTopRisingStocks(Market market, int limit);

    List<MarketRankingStock> findVolumeSurgeStocks(Market market, int limit);
}
