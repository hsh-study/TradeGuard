package seokhoon.trade.adapter.persistence;

import jakarta.persistence.*;
import seokhoon.trade.domain.order.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "live_order_requests")
class LiveOrderRequestEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(name = "signal_id") Long signalId;
    @Column(name = "stock_code", nullable = false) String stockCode;
    @Enumerated(EnumType.STRING) @Column(nullable = false) OrderSide side;
    @Column(nullable = false) int quantity;
    @Column(name = "order_price", nullable = false) BigDecimal orderPrice;
    @Enumerated(EnumType.STRING) @Column(name = "order_type", nullable = false) OrderType orderType;
    @Enumerated(EnumType.STRING) @Column(nullable = false) LiveOrderStatus status;
    @Column(name = "kis_order_no") String kisOrderNo;
    @Column(name = "kis_original_order_no") String kisOriginalOrderNo;
    @Column(name = "failure_reason") String failureReason;
    @Column(name = "requested_at", nullable = false) Instant requestedAt;
    @Column(name = "submitted_at") Instant submittedAt;
    @Column(name = "updated_at", nullable = false) Instant updatedAt;
    @Column(name = "remaining_quantity", nullable = false) int remainingQuantity;
    @Column(name = "filled_quantity", nullable = false) int filledQuantity;
    @Column(name = "last_inquired_at") Instant lastInquiredAt;
    @Column(name = "cancel_requested_at") Instant cancelRequestedAt;
    @Column(name = "canceled_at") Instant canceledAt;
    @Column(name = "expire_at") Instant expireAt;

    protected LiveOrderRequestEntity() {}

    static LiveOrderRequestEntity from(LiveOrderRequest value) {
        LiveOrderRequestEntity entity = new LiveOrderRequestEntity();
        entity.update(value);
        return entity;
    }

    void update(LiveOrderRequest value) {
        signalId = value.signalId(); stockCode = value.stockCode(); side = value.side();
        quantity = value.quantity(); orderPrice = value.orderPrice();
        orderType = value.orderType(); status = value.status();
        kisOrderNo = value.kisOrderNo(); kisOriginalOrderNo = value.kisOriginalOrderNo();
        failureReason = value.failureReason(); requestedAt = value.requestedAt();
        submittedAt = value.submittedAt(); updatedAt = value.updatedAt();
        remainingQuantity = value.remainingQuantity();
        filledQuantity = value.filledQuantity();
        lastInquiredAt = value.lastInquiredAt();
        cancelRequestedAt = value.cancelRequestedAt();
        canceledAt = value.canceledAt();
        expireAt = value.expireAt();
    }

    LiveOrderRequest toDomain() {
        return new LiveOrderRequest(id, signalId, stockCode, side, quantity,
                orderPrice, orderType, status, kisOrderNo, kisOriginalOrderNo,
                failureReason, requestedAt, submittedAt, updatedAt,
                remainingQuantity, filledQuantity, lastInquiredAt,
                cancelRequestedAt, canceledAt, expireAt);
    }
}
