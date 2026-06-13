package seokhoon.trade.domain.order;

import java.time.Instant;

public record LiveOrderStatusHistory(
        Long id,
        long liveOrderRequestId,
        LiveOrderStatus fromStatus,
        LiveOrderStatus toStatus,
        String reason,
        Instant createdAt
) {
}
