package seokhoon.trade.application.port.in;

public interface RetryBrokerFailedOrderUseCase {
    BrokerOrderRetryResult retry(long orderId);
}
