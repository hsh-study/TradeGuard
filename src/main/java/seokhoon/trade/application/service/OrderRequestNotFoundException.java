package seokhoon.trade.application.service;

public class OrderRequestNotFoundException extends RuntimeException {
    public OrderRequestNotFoundException(long orderId) {
        super("Order request not found: " + orderId);
    }
}
