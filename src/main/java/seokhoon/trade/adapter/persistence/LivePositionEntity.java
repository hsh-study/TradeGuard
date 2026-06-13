package seokhoon.trade.adapter.persistence;

import jakarta.persistence.*;
import seokhoon.trade.domain.position.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "live_positions")
class LivePositionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(name = "stock_code", nullable = false) String stockCode;
    @Column(nullable = false) int quantity;
    @Column(name = "average_buy_price", nullable = false) BigDecimal averageBuyPrice;
    @Column(name = "buy_amount", nullable = false) BigDecimal buyAmount;
    @Column(name = "buy_commission", nullable = false) BigDecimal buyCommission;
    @Enumerated(EnumType.STRING) @Column(nullable = false) LivePositionStatus status;
    @Column(name = "opened_at", nullable = false) Instant openedAt;
    @Column(name = "closed_at") Instant closedAt;
    protected LivePositionEntity() {}
    static LivePositionEntity from(LivePosition value) { var e = new LivePositionEntity(); e.update(value); return e; }
    void update(LivePosition v) { stockCode=v.stockCode(); quantity=v.quantity(); averageBuyPrice=v.averageBuyPrice(); buyAmount=v.buyAmount(); buyCommission=v.buyCommission(); status=v.status(); openedAt=v.openedAt(); closedAt=v.closedAt(); }
    LivePosition toDomain() { return new LivePosition(id, stockCode, quantity, averageBuyPrice, buyAmount, buyCommission, status, openedAt, closedAt); }
}
