package seokhoon.trade.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import seokhoon.trade.application.port.in.BrokerOrderRetryResult;
import seokhoon.trade.application.port.in.RetryBrokerFailedOrderUseCase;
import seokhoon.trade.application.port.out.BrokerPort;
import seokhoon.trade.application.port.out.OrderRequestPort;
import seokhoon.trade.domain.order.OrderRequest;
import seokhoon.trade.domain.order.OrderStatus;

import java.time.Clock;
import java.time.Instant;

@Service
public class BrokerFailedOrderRetryService implements RetryBrokerFailedOrderUseCase {
    private final OrderRequestPort orderRequestPort;
    private final BrokerPort brokerPort;
    private final Clock clock;

    @Autowired
    public BrokerFailedOrderRetryService(
            OrderRequestPort orderRequestPort,
            BrokerPort brokerPort
    ) {
        this(orderRequestPort, brokerPort, Clock.systemUTC());
    }

    BrokerFailedOrderRetryService(
            OrderRequestPort orderRequestPort,
            BrokerPort brokerPort,
            Clock clock
    ) {
        this.orderRequestPort = orderRequestPort;
        this.brokerPort = brokerPort;
        this.clock = clock;
    }

    @Override
    public BrokerOrderRetryResult retry(long orderId) {
        OrderRequest orderRequest = orderRequestPort.findById(orderId)
                .orElseThrow(() -> new OrderRequestNotFoundException(orderId));
        validateRetry(orderRequest);

        if (!orderRequestPort.claimRetry(orderId)) {
            throw new OrderRetryNotAllowedException(
                    "Order retry is already in progress or no longer allowed"
            );
        }
        orderRequest.markRetryRequested();

        try {
            OrderRequest accepted = brokerPort.requestOrder(orderRequest);
            return BrokerOrderRetryResult.accepted(
                    orderId,
                    orderRequestPort.updateById(orderId, accepted)
            );
        } catch (RuntimeException exception) {
            orderRequest.markBrokerFailed(
                    brokerFailureReason(exception),
                    Instant.now(clock),
                    isRetryableBrokerFailure(exception)
            );
            return BrokerOrderRetryResult.brokerFailed(
                    orderId,
                    orderRequestPort.updateById(orderId, orderRequest)
            );
        }
    }

    private static void validateRetry(OrderRequest orderRequest) {
        if (orderRequest.status() != OrderStatus.BROKER_FAILED) {
            throw new OrderRetryNotAllowedException(
                    "Only BROKER_FAILED orders can be retried"
            );
        }
        if (!orderRequest.retryable()) {
            throw new OrderRetryNotAllowedException("Order is not retryable");
        }
    }

    private static String brokerFailureReason(RuntimeException exception) {
        String message = exception.getMessage();
        String reason = message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : message;
        return reason.length() <= 1000 ? reason : reason.substring(0, 1000);
    }

    private static boolean isRetryableBrokerFailure(RuntimeException exception) {
        return !(exception instanceof IllegalArgumentException)
                && !(exception instanceof UnsupportedOperationException);
    }
}
