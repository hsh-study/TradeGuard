package seokhoon.trade.domain.order;

public enum OrderStatus {
    CREATED,
    REQUESTED,
    ACCEPTED,
    REJECTED,
    BROKER_FAILED,
    RETRY_REQUESTED,
    CANCELED,
    FILLED,
    PARTIALLY_FILLED
}
