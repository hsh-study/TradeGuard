package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.order.OrderRequest;

public record BrokerOrderRetryResult(
        long orderId,
        OrderRequest orderRequest,
        boolean brokerFailed
) {
    public static BrokerOrderRetryResult accepted(long orderId, OrderRequest orderRequest) {
        return new BrokerOrderRetryResult(orderId, orderRequest, false);
    }

    public static BrokerOrderRetryResult brokerFailed(long orderId, OrderRequest orderRequest) {
        return new BrokerOrderRetryResult(orderId, orderRequest, true);
    }
}
