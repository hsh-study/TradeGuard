package seokhoon.trade.adapter.persistence;

import jakarta.persistence.*;
import seokhoon.trade.domain.order.*;

import java.time.Instant;

@Entity
@Table(name = "live_order_cancel_requests")
class LiveOrderCancelRequestEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(name = "live_order_request_id", nullable = false) long orderId;
    @Column(name = "kis_original_order_no") String kisOriginalOrderNo;
    @Column(name = "cancel_quantity", nullable = false) int cancelQuantity;
    @Enumerated(EnumType.STRING) @Column(nullable = false) LiveOrderCancelStatus status;
    @Column(name = "kis_cancel_order_no") String kisCancelOrderNo;
    @Column(name = "failure_reason") String failureReason;
    @Column(nullable = false) String reason;
    @Column(name = "requested_at", nullable = false) Instant requestedAt;
    @Column(name = "submitted_at") Instant submittedAt;
    @Column(name = "updated_at", nullable = false) Instant updatedAt;

    protected LiveOrderCancelRequestEntity() {}

    static LiveOrderCancelRequestEntity from(LiveOrderCancelRequest value) {
        LiveOrderCancelRequestEntity entity = new LiveOrderCancelRequestEntity();
        entity.update(value);
        return entity;
    }

    void update(LiveOrderCancelRequest value) {
        orderId=value.liveOrderRequestId();
        kisOriginalOrderNo=value.kisOriginalOrderNo();
        cancelQuantity=value.cancelQuantity();
        status=value.status();
        kisCancelOrderNo=value.kisCancelOrderNo();
        failureReason=value.failureReason();
        reason=value.reason();
        requestedAt=value.requestedAt();
        submittedAt=value.submittedAt();
        updatedAt=value.updatedAt();
    }

    LiveOrderCancelRequest toDomain() {
        return new LiveOrderCancelRequest(id,orderId,kisOriginalOrderNo,
                cancelQuantity,status,kisCancelOrderNo,failureReason,reason,
                requestedAt,submittedAt,updatedAt);
    }
}
