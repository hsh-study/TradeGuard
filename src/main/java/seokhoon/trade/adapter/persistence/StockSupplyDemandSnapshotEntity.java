package seokhoon.trade.adapter.persistence;

import jakarta.persistence.*;
import seokhoon.trade.domain.market.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "stock_supply_demand_snapshots", uniqueConstraints = @UniqueConstraint(
        name = "uk_stock_supply_demand", columnNames = {"stock_code", "trade_date"}))
class StockSupplyDemandSnapshotEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(name="stock_code", nullable=false, length=20) String stockCode;
    @Column(name="trade_date", nullable=false) LocalDate tradeDate;
    @Column(name="foreign_net_buy_amount", nullable=false, precision=19, scale=4) BigDecimal foreignNetBuyAmount;
    @Column(name="institution_net_buy_amount", nullable=false, precision=19, scale=4) BigDecimal institutionNetBuyAmount;
    @Column(name="individual_net_buy_amount", nullable=false, precision=19, scale=4) BigDecimal individualNetBuyAmount;
    @Column(name="consecutive_foreign_buy_days", nullable=false) int consecutiveForeignBuyDays;
    @Column(name="consecutive_institution_buy_days", nullable=false) int consecutiveInstitutionBuyDays;
    @Column(name="consecutive_combined_smart_money_buy_days", nullable=false) int consecutiveCombinedSmartMoneyBuyDays;
    @Column(name="smart_money_net_buy_amount", nullable=false, precision=19, scale=4) BigDecimal smartMoneyNetBuyAmount;
    @Column(name="smart_money_5day_net_buy_amount", nullable=false, precision=19, scale=4) BigDecimal smartMoney5dayNetBuyAmount;
    @Column(name="individual_dominance_ratio", nullable=false, precision=10, scale=6) BigDecimal individualDominanceRatio;
    @Column(name="supply_demand_score", nullable=false) int supplyDemandScore;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=40) SupplyDemandStatus status;
    @Column(nullable=false, columnDefinition="TEXT") String reasons;
    @Column(name="created_at", nullable=false) Instant createdAt;
    @Column(name="updated_at", nullable=false) Instant updatedAt;

    void update(StockSupplyDemandSnapshot v) {
        stockCode=v.stockCode(); tradeDate=v.tradeDate(); foreignNetBuyAmount=v.foreignNetBuyAmount();
        institutionNetBuyAmount=v.institutionNetBuyAmount(); individualNetBuyAmount=v.individualNetBuyAmount();
        consecutiveForeignBuyDays=v.consecutiveForeignBuyDays(); consecutiveInstitutionBuyDays=v.consecutiveInstitutionBuyDays();
        consecutiveCombinedSmartMoneyBuyDays=v.consecutiveCombinedSmartMoneyBuyDays(); smartMoneyNetBuyAmount=v.smartMoneyNetBuyAmount();
        smartMoney5dayNetBuyAmount=v.smartMoney5dayNetBuyAmount(); individualDominanceRatio=v.individualDominanceRatio();
        supplyDemandScore=v.supplyDemandScore(); status=v.status(); reasons=String.join("\n", v.reasons());
        if (createdAt == null) createdAt=v.createdAt(); updatedAt=v.updatedAt();
    }
    StockSupplyDemandSnapshot toDomain() { List<String> parsed = reasons == null || reasons.isBlank()
            ? List.of() : Arrays.asList(reasons.split("\\n"));
        return new StockSupplyDemandSnapshot(id, stockCode, tradeDate, foreignNetBuyAmount,
            institutionNetBuyAmount, individualNetBuyAmount, consecutiveForeignBuyDays,
            consecutiveInstitutionBuyDays, consecutiveCombinedSmartMoneyBuyDays, smartMoneyNetBuyAmount,
            smartMoney5dayNetBuyAmount, individualDominanceRatio, supplyDemandScore, status, parsed, createdAt, updatedAt); }
}
