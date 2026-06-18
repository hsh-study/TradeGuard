package seokhoon.trade.adapter.research.market;

import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.out.InvestorFlowProviderPort;
import seokhoon.trade.application.port.out.InvestorFlowDiagnosticPort;
import seokhoon.trade.domain.market.MarketInvestorFlow;
import seokhoon.trade.domain.market.StockInvestorFlow;
import seokhoon.trade.domain.market.InvestorFlowMarket;
import seokhoon.trade.domain.market.InvestorFlowFetchResult;
import seokhoon.trade.domain.market.InvestorFlowDiagnosticData;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import java.time.LocalDate;

@Component
@ConditionalOnProperty(name = "tradeguard.investor-flow.provider-enabled",
        havingValue = "false", matchIfMissing = true)
public class DisabledInvestorFlowProviderAdapter implements InvestorFlowProviderPort, InvestorFlowDiagnosticPort {
    @Override public InvestorFlowFetchResult<StockInvestorFlow> fetchStockInvestorFlows(String stockCode, LocalDate tradeDate){return InvestorFlowFetchResult.empty();}
    @Override public InvestorFlowFetchResult<MarketInvestorFlow> fetchMarketInvestorFlows(InvestorFlowMarket market, LocalDate tradeDate){return InvestorFlowFetchResult.empty();}
    @Override public InvestorFlowDiagnosticData diagnoseStock(String stockCode,LocalDate tradeDate){throw new UnsupportedOperationException("KIS investor flow provider is disabled");}
    @Override public InvestorFlowDiagnosticData diagnoseMarket(InvestorFlowMarket market,LocalDate tradeDate){throw new UnsupportedOperationException("KIS investor flow provider is disabled");}
}
