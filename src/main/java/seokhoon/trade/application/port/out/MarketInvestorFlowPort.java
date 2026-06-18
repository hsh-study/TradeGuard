package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.market.MarketInvestorFlow;
import seokhoon.trade.domain.market.InvestorFlowMarket;
import java.time.LocalDate;
import java.util.List;

public interface MarketInvestorFlowPort {
    List<MarketInvestorFlow> saveAll(List<MarketInvestorFlow> flows);
    List<MarketInvestorFlow> findByMarketAndDate(InvestorFlowMarket market, LocalDate tradeDate);
    List<MarketInvestorFlow> findRecentByMarket(InvestorFlowMarket market, LocalDate endDate, int days);
}
