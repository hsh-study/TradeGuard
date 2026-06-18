package seokhoon.trade.domain.market;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record MarketInvestorFlow(Long id, InvestorFlowMarket market, LocalDate tradeDate,
        InvestorType investorType, String rawInvestorType, BigDecimal netBuyAmount,
        Long netBuyQuantity, BigDecimal buyAmount, BigDecimal sellAmount,
        InvestorFlowSource source, Instant createdAt, Instant updatedAt) {
}
