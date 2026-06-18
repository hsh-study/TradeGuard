package seokhoon.trade.adapter.persistence;

import jakarta.persistence.*;
import seokhoon.trade.domain.market.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "stock_investor_flows", uniqueConstraints = @UniqueConstraint(
        name = "uk_stock_investor_flow", columnNames = {"stock_code", "trade_date", "investor_type", "source"}))
class StockInvestorFlowEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(name = "stock_code", nullable = false, length = 20) String stockCode;
    @Column(name = "trade_date", nullable = false) LocalDate tradeDate;
    @Enumerated(EnumType.STRING) @Column(name = "investor_type", nullable = false, length = 40) InvestorType investorType;
    @Column(name = "raw_investor_type", length = 100) String rawInvestorType;
    @Column(name = "net_buy_amount", precision = 19, scale = 4) BigDecimal netBuyAmount;
    @Column(name = "net_buy_quantity") Long netBuyQuantity;
    @Column(name = "buy_amount", precision = 19, scale = 4) BigDecimal buyAmount;
    @Column(name = "sell_amount", precision = 19, scale = 4) BigDecimal sellAmount;
    @Column(name = "buy_quantity") Long buyQuantity;
    @Column(name = "sell_quantity") Long sellQuantity;
    @Enumerated(EnumType.STRING) @Column(name = "source", nullable = false, length = 20) InvestorFlowSource source;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "updated_at", nullable = false) Instant updatedAt;

    void update(StockInvestorFlow value) {
        stockCode=value.stockCode(); tradeDate=value.tradeDate(); investorType=value.investorType();
        rawInvestorType=value.rawInvestorType(); netBuyAmount=value.netBuyAmount(); netBuyQuantity=value.netBuyQuantity();
        buyAmount=value.buyAmount(); sellAmount=value.sellAmount(); buyQuantity=value.buyQuantity();
        sellQuantity=value.sellQuantity(); source=value.source();
        if (createdAt == null) createdAt=value.createdAt(); updatedAt=value.updatedAt();
    }
    StockInvestorFlow toDomain() { return new StockInvestorFlow(id, stockCode, tradeDate, investorType,
            rawInvestorType, netBuyAmount, netBuyQuantity, buyAmount, sellAmount, buyQuantity,
            sellQuantity, source, createdAt, updatedAt); }
}
