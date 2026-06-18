package seokhoon.trade.adapter.research.market;

import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.out.InvestorFlowProviderPort;
import seokhoon.trade.domain.market.MarketInvestorFlow;
import seokhoon.trade.domain.market.StockInvestorFlow;
import seokhoon.trade.domain.market.InvestorFlowMarket;
import java.time.LocalDate;
import java.util.List;

@Component
public class DisabledInvestorFlowProviderAdapter implements InvestorFlowProviderPort {
    // KIS investor-flow TR IDs must be verified against the target environment before enabling an adapter.
    @Override public List<StockInvestorFlow> fetchStockInvestorFlows(String stockCode, LocalDate tradeDate){return List.of();}
    @Override public List<MarketInvestorFlow> fetchMarketInvestorFlows(InvestorFlowMarket market, LocalDate tradeDate){return List.of();}
}
