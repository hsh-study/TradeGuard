package seokhoon.trade.domain.order;

import java.math.BigDecimal;
import java.time.Instant;

public record LiveOrderRequest(
        Long id,
        Long signalId,
        String stockCode,
        OrderSide side,
        int quantity,
        BigDecimal orderPrice,
        OrderType orderType,
        LiveOrderStatus status,
        String kisOrderNo,
        String kisOriginalOrderNo,
        String failureReason,
        Instant requestedAt,
        Instant submittedAt,
        Instant updatedAt
) {
    public LiveOrderRequest withStatus(
            LiveOrderStatus next,
            String orderNo,
            String originalOrderNo,
            String failure,
            Instant time
    ) {
        return new LiveOrderRequest(
                id, signalId, stockCode, side, quantity, orderPrice, orderType,
                next, orderNo, originalOrderNo, failure, requestedAt,
                next == LiveOrderStatus.SUBMITTED || next == LiveOrderStatus.ACCEPTED
                        ? time : submittedAt,
                time
        );
    }
}
