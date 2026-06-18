package seokhoon.trade.adapter.persistence;

import jakarta.persistence.*;
import seokhoon.trade.domain.market.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "market_investor_flows", uniqueConstraints = @UniqueConstraint(
        name = "uk_market_investor_flow", columnNames = {"market", "trade_date", "investor_type", "source"}))
class MarketInvestorFlowEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) InvestorFlowMarket market;
    @Column(name = "trade_date", nullable = false) LocalDate tradeDate;
    @Enumerated(EnumType.STRING) @Column(name = "investor_type", nullable = false, length = 40) InvestorType investorType;
    @Column(name = "raw_investor_type", length = 100) String rawInvestorType;
    @Column(name = "net_buy_amount", nullable = false, precision = 19, scale = 4) BigDecimal netBuyAmount;
    @Column(name = "net_buy_quantity") Long netBuyQuantity;
    @Column(name = "buy_amount", precision = 19, scale = 4) BigDecimal buyAmount;
    @Column(name = "sell_amount", precision = 19, scale = 4) BigDecimal sellAmount;
    @Enumerated(EnumType.STRING) @Column(name = "source", nullable = false, length = 20) InvestorFlowSource source;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "updated_at", nullable = false) Instant updatedAt;

    void update(MarketInvestorFlow value) {
        market=value.market(); tradeDate=value.tradeDate(); investorType=value.investorType();
        rawInvestorType=value.rawInvestorType(); netBuyAmount=value.netBuyAmount(); netBuyQuantity=value.netBuyQuantity();
        buyAmount=value.buyAmount(); sellAmount=value.sellAmount(); source=value.source();
        if (createdAt == null) createdAt=value.createdAt(); updatedAt=value.updatedAt();
    }
    MarketInvestorFlow toDomain() { return new MarketInvestorFlow(id, market, tradeDate, investorType,
            rawInvestorType, netBuyAmount, netBuyQuantity, buyAmount, sellAmount, source, createdAt, updatedAt); }
}
