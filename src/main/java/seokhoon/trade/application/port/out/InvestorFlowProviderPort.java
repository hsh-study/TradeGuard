package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.market.MarketInvestorFlow;
import seokhoon.trade.domain.market.StockInvestorFlow;
import seokhoon.trade.domain.market.InvestorFlowMarket;
import java.time.LocalDate;
import java.util.List;

public interface InvestorFlowProviderPort {
    List<StockInvestorFlow> fetchStockInvestorFlows(String stockCode, LocalDate tradeDate);
    List<MarketInvestorFlow> fetchMarketInvestorFlows(InvestorFlowMarket market, LocalDate tradeDate);
}
