package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.market.InvestorFlowMarket;
import seokhoon.trade.domain.market.InvestorFlowVerification;

import java.time.LocalDate;

public interface VerifyInvestorFlowProviderUseCase {
    InvestorFlowVerification verifyStock(String stockCode, LocalDate tradeDate);
    InvestorFlowVerification verifyMarket(InvestorFlowMarket market, LocalDate tradeDate);
}
