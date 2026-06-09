package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.order.OrderStatus;
import seokhoon.trade.domain.audit.AuditActor;

import java.time.Instant;

public record OrderStatusHistoryView(
        Long historyId,
        long orderId,
        OrderStatus fromStatus,
        OrderStatus toStatus,
        String reason,
        AuditActor actor,
        String requestCorrelationId,
        Instant createdAt
) {
}
