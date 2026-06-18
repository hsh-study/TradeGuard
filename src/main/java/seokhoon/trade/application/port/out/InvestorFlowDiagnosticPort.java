package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.market.InvestorFlowDiagnosticData;
import seokhoon.trade.domain.market.InvestorFlowMarket;

import java.time.LocalDate;

public interface InvestorFlowDiagnosticPort {
    InvestorFlowDiagnosticData diagnoseStock(String stockCode, LocalDate tradeDate);
    InvestorFlowDiagnosticData diagnoseMarket(InvestorFlowMarket market, LocalDate tradeDate);
}
