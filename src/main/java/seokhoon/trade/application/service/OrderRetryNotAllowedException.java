package seokhoon.trade.application.service;

public class OrderRetryNotAllowedException extends IllegalArgumentException {
    public OrderRetryNotAllowedException(String message) {
        super(message);
    }
}
