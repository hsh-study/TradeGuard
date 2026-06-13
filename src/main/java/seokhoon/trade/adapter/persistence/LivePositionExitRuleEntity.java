package seokhoon.trade.adapter.persistence;

import jakarta.persistence.*;
import seokhoon.trade.domain.position.LivePositionExitRule;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "live_position_exit_rules")
class LivePositionExitRuleEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(name="position_id", nullable=false) long positionId;
    @Column(name="take_profit_rate", nullable=false) BigDecimal takeProfitRate;
    @Column(name="stop_loss_rate", nullable=false) BigDecimal stopLossRate;
    @Column(name="max_loss_amount", nullable=false) BigDecimal maxLossAmount;
    @Column(name="sell_tax_rate", nullable=false) BigDecimal sellTaxRate;
    @Column(name="buy_commission_rate", nullable=false) BigDecimal buyCommissionRate;
    @Column(name="sell_commission_rate", nullable=false) BigDecimal sellCommissionRate;
    @Column(nullable=false) boolean enabled;
    @Column(name="created_at", nullable=false) Instant createdAt;
    @Column(name="updated_at", nullable=false) Instant updatedAt;
    protected LivePositionExitRuleEntity() {}
    static LivePositionExitRuleEntity from(LivePositionExitRule v) { var e=new LivePositionExitRuleEntity(); e.update(v); e.createdAt=v.createdAt(); return e; }
    void update(LivePositionExitRule v) { positionId=v.positionId(); takeProfitRate=v.takeProfitRate(); stopLossRate=v.stopLossRate(); maxLossAmount=v.maxLossAmount(); sellTaxRate=v.sellTaxRate(); buyCommissionRate=v.buyCommissionRate(); sellCommissionRate=v.sellCommissionRate(); enabled=v.enabled(); updatedAt=v.updatedAt(); }
    LivePositionExitRule toDomain() { return new LivePositionExitRule(id, positionId, takeProfitRate, stopLossRate, maxLossAmount, sellTaxRate, buyCommissionRate, sellCommissionRate, enabled, createdAt, updatedAt); }
}
