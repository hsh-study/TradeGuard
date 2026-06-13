package seokhoon.trade.domain.order;

import java.time.Instant;

public record LiveOrderCancelRequest(
        Long id,
        long liveOrderRequestId,
        String kisOriginalOrderNo,
        int cancelQuantity,
        LiveOrderCancelStatus status,
        String kisCancelOrderNo,
        String failureReason,
        String reason,
        Instant requestedAt,
        Instant submittedAt,
        Instant updatedAt
) {
    public LiveOrderCancelRequest withResult(LiveOrderCancelStatus next,
            String cancelOrderNo, String failure, Instant time) {
        return new LiveOrderCancelRequest(id, liveOrderRequestId,
                kisOriginalOrderNo, cancelQuantity, next, cancelOrderNo,
                failure, reason, requestedAt, time, time);
    }
}
