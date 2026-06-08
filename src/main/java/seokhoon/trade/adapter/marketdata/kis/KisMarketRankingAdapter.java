package seokhoon.trade.adapter.marketdata.kis;

import seokhoon.trade.application.port.out.MarketRankingPort;
import seokhoon.trade.application.port.out.MarketRankingStock;
import seokhoon.trade.domain.stock.Market;

import java.util.List;

// TODO: Implement read-only KIS ranking endpoints for trading value, rising rate, and volume surge.
// This class is intentionally not registered as a Spring bean for the MVP.
public class KisMarketRankingAdapter implements MarketRankingPort {
    @Override
    public List<MarketRankingStock> findTopTradingValueStocks(Market market, int limit) {
        throw new UnsupportedOperationException("KIS market ranking API is not implemented in MVP");
    }

    @Override
    public List<MarketRankingStock> findTopRisingStocks(Market market, int limit) {
        throw new UnsupportedOperationException("KIS market ranking API is not implemented in MVP");
    }

    @Override
    public List<MarketRankingStock> findVolumeSurgeStocks(Market market, int limit) {
        throw new UnsupportedOperationException("KIS market ranking API is not implemented in MVP");
    }
}
