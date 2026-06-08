package seokhoon.trade.application.port.out;

public interface NotificationPort {
    NotificationDeliveryResult send(NotificationMessage message);
}
