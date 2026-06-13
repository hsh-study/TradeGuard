package seokhoon.trade.domain.order;

public enum LiveOrderStatus {
    CREATED, RISK_APPROVED, SUBMITTED, ACCEPTED, REJECTED,
    FILLED, PARTIALLY_FILLED, CANCEL_REQUESTED, CANCELED, FAILED
}
