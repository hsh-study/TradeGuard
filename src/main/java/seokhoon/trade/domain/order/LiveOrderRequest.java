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
        Instant updatedAt,
        int remainingQuantity,
        int filledQuantity,
        Instant lastInquiredAt,
        Instant cancelRequestedAt,
        Instant canceledAt,
        Instant expireAt
) {
    public LiveOrderRequest(Long id, Long signalId, String stockCode,
            OrderSide side, int quantity, BigDecimal orderPrice,
            OrderType orderType, LiveOrderStatus status, String kisOrderNo,
            String kisOriginalOrderNo, String failureReason,
            Instant requestedAt, Instant submittedAt, Instant updatedAt) {
        this(id, signalId, stockCode, side, quantity, orderPrice, orderType,
                status, kisOrderNo, kisOriginalOrderNo, failureReason,
                requestedAt, submittedAt, updatedAt,
                status == LiveOrderStatus.FILLED
                        || status == LiveOrderStatus.CANCELED ? 0 : quantity,
                status == LiveOrderStatus.FILLED ? quantity : 0,
                null, null, null, null);
    }

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
                time, remainingQuantity, filledQuantity, lastInquiredAt,
                cancelRequestedAt, canceledAt, expireAt
        );
    }

    public LiveOrderRequest withExpireAt(Instant value, Instant time) {
        return new LiveOrderRequest(id, signalId, stockCode, side, quantity,
                orderPrice, orderType, status, kisOrderNo, kisOriginalOrderNo,
                failureReason, requestedAt, submittedAt, time,
                remainingQuantity, filledQuantity, lastInquiredAt,
                cancelRequestedAt, canceledAt, value);
    }

    public LiveOrderRequest withExecution(int filled, int remaining,
            LiveOrderStatus next, Instant inquiredAt) {
        return new LiveOrderRequest(id, signalId, stockCode, side, quantity,
                orderPrice, orderType, next, kisOrderNo, kisOriginalOrderNo,
                failureReason, requestedAt, submittedAt, inquiredAt,
                remaining, filled, inquiredAt, cancelRequestedAt,
                next == LiveOrderStatus.CANCELED ? inquiredAt : canceledAt,
                expireAt);
    }

    public LiveOrderRequest withCancelRequested(Instant time) {
        return new LiveOrderRequest(id, signalId, stockCode, side, quantity,
                orderPrice, orderType, LiveOrderStatus.CANCEL_REQUESTED,
                kisOrderNo, kisOriginalOrderNo, failureReason, requestedAt,
                submittedAt, time, remainingQuantity, filledQuantity,
                lastInquiredAt, time, canceledAt, expireAt);
    }

    public LiveOrderRequest withCanceled(Instant time) {
        return new LiveOrderRequest(id, signalId, stockCode, side, quantity,
                orderPrice, orderType, LiveOrderStatus.CANCELED, kisOrderNo,
                kisOriginalOrderNo, failureReason, requestedAt, submittedAt,
                time, 0, filledQuantity, lastInquiredAt, cancelRequestedAt,
                time, expireAt);
    }

    public LiveOrderRequest withCancellationAccepted(int canceledQuantity,
            Instant time) {
        int remaining = Math.max(0, remainingQuantity - canceledQuantity);
        LiveOrderStatus next = remaining == 0 ? LiveOrderStatus.CANCELED
                : filledQuantity > 0 ? LiveOrderStatus.PARTIALLY_FILLED
                : LiveOrderStatus.ACCEPTED;
        return new LiveOrderRequest(id, signalId, stockCode, side, quantity,
                orderPrice, orderType, next, kisOrderNo, kisOriginalOrderNo,
                failureReason, requestedAt, submittedAt, time, remaining,
                filledQuantity, lastInquiredAt, cancelRequestedAt,
                next == LiveOrderStatus.CANCELED ? time : canceledAt, expireAt);
    }
}
