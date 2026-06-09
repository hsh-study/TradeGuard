package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.order.OrderStatus;

import java.time.Instant;

public record OrderStatusHistoryRecord(
        Long id,
        long orderRequestId,
        OrderStatus fromStatus,
        OrderStatus toStatus,
        String reason,
        Instant createdAt
) {
}
