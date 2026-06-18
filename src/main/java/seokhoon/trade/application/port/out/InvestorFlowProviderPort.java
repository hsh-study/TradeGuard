package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.market.MarketInvestorFlow;
import seokhoon.trade.domain.market.StockInvestorFlow;
import seokhoon.trade.domain.market.InvestorFlowMarket;
import seokhoon.trade.domain.market.InvestorFlowFetchResult;
import java.time.LocalDate;
import java.util.List;

public interface InvestorFlowProviderPort {
    InvestorFlowFetchResult<StockInvestorFlow> fetchStockInvestorFlows(String stockCode, LocalDate tradeDate);
    InvestorFlowFetchResult<MarketInvestorFlow> fetchMarketInvestorFlows(InvestorFlowMarket market, LocalDate tradeDate);
}
