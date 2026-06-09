package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.order.OrderStatus;

import java.time.Instant;

public record OrderStatusHistoryView(
        Long historyId,
        long orderId,
        OrderStatus fromStatus,
        OrderStatus toStatus,
        String reason,
        Instant createdAt
) {
}
