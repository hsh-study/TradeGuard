package seokhoon.trade.adapter.research.market;

import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.out.InvestorFlowProviderPort;
import seokhoon.trade.domain.market.MarketInvestorFlow;
import seokhoon.trade.domain.market.StockInvestorFlow;
import seokhoon.trade.domain.market.InvestorFlowMarket;
import seokhoon.trade.domain.market.InvestorFlowFetchResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import java.time.LocalDate;
import java.util.List;

@Component
@ConditionalOnProperty(name = "tradeguard.investor-flow.provider-enabled",
        havingValue = "false", matchIfMissing = true)
public class DisabledInvestorFlowProviderAdapter implements InvestorFlowProviderPort {
    @Override public InvestorFlowFetchResult<StockInvestorFlow> fetchStockInvestorFlows(String stockCode, LocalDate tradeDate){return InvestorFlowFetchResult.empty();}
    @Override public InvestorFlowFetchResult<MarketInvestorFlow> fetchMarketInvestorFlows(InvestorFlowMarket market, LocalDate tradeDate){return InvestorFlowFetchResult.empty();}
}
