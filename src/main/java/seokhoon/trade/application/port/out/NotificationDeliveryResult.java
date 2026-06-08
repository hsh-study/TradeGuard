package seokhoon.trade.application.port.out;

public record NotificationDeliveryResult(boolean sent, String message) {
    public static NotificationDeliveryResult success() {
        return new NotificationDeliveryResult(true, "sent");
    }

    public static NotificationDeliveryResult skipped(String message) {
        return new NotificationDeliveryResult(false, message);
    }
}
