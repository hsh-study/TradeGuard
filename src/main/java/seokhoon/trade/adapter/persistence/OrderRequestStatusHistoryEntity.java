package seokhoon.trade.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import seokhoon.trade.application.port.out.OrderStatusHistoryRecord;
import seokhoon.trade.domain.audit.AuditActor;
import seokhoon.trade.domain.order.OrderStatus;

import java.time.Instant;

@Entity
@Table(name = "order_request_status_histories")
public class OrderRequestStatusHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "order_request_id", nullable = false)
    private long orderRequestId;
    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", nullable = false)
    private OrderStatus fromStatus;
    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    private OrderStatus toStatus;
    @Column(length = 1000)
    private String reason;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AuditActor actor;
    @Column(name = "request_correlation_id", length = 128)
    private String requestCorrelationId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected OrderRequestStatusHistoryEntity() {
    }

    OrderRequestStatusHistoryEntity(
            long orderRequestId,
            OrderStatus fromStatus,
            OrderStatus toStatus,
            String reason,
            AuditActor actor,
            String requestCorrelationId,
            Instant createdAt
    ) {
        this.orderRequestId = orderRequestId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = reason;
        this.actor = actor;
        this.requestCorrelationId = requestCorrelationId;
        this.createdAt = createdAt;
    }

    OrderStatusHistoryRecord toRecord() {
        return new OrderStatusHistoryRecord(
                id,
                orderRequestId,
                fromStatus,
                toStatus,
                reason,
                actor,
                requestCorrelationId,
                createdAt
        );
    }
}
