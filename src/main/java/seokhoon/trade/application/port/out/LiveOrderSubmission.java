package seokhoon.trade.application.port.out;

public record LiveOrderSubmission(
        boolean accepted,
        String orderNo,
        String originalOrderNo,
        String failureReason
) {
    public static LiveOrderSubmission accepted(String orderNo, String originalOrderNo) {
        return new LiveOrderSubmission(true, orderNo, originalOrderNo, null);
    }

    public static LiveOrderSubmission rejected(String reason) {
        return new LiveOrderSubmission(false, null, null, reason);
    }
}
