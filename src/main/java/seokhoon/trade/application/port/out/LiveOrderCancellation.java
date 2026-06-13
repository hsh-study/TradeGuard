package seokhoon.trade.application.port.out;

public record LiveOrderCancellation(
        boolean accepted,
        String cancelOrderNo,
        String failureReason
) {
    public static LiveOrderCancellation accepted(String orderNo) {
        return new LiveOrderCancellation(true, orderNo, null);
    }

    public static LiveOrderCancellation rejected(String reason) {
        return new LiveOrderCancellation(false, null, reason);
    }
}
