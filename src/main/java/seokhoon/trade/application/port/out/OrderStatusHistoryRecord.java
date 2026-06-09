package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.order.OrderStatus;
import seokhoon.trade.domain.audit.AuditActor;

import java.time.Instant;

public record OrderStatusHistoryRecord(
        Long id,
        long orderRequestId,
        OrderStatus fromStatus,
        OrderStatus toStatus,
        String reason,
        AuditActor actor,
        String requestCorrelationId,
        Instant createdAt
) {
}
