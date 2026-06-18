package seokhoon.trade.domain.market;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record StockInvestorFlow(Long id, String stockCode, LocalDate tradeDate,
        InvestorType investorType, String rawInvestorType, BigDecimal netBuyAmount,
        Long netBuyQuantity, BigDecimal buyAmount, BigDecimal sellAmount,
        Long buyQuantity, Long sellQuantity, InvestorFlowSource source,
        Instant createdAt, Instant updatedAt) {
}
