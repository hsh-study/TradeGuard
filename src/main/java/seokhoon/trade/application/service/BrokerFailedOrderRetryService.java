package seokhoon.trade.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.in.BrokerOrderRetryResult;
import seokhoon.trade.application.port.in.RetryBrokerFailedOrderUseCase;
import seokhoon.trade.application.port.out.BrokerPort;
import seokhoon.trade.application.port.out.CorrelationIdProvider;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.application.port.out.OrderRequestPort;
import seokhoon.trade.application.port.out.OrderStatusHistoryPort;
import seokhoon.trade.application.port.out.SignalStatusHistoryPort;
import seokhoon.trade.application.port.out.TradingSignalPort;
import seokhoon.trade.domain.order.OrderRequest;
import seokhoon.trade.domain.order.OrderStatus;
import seokhoon.trade.domain.strategy.TradingSignalStatus;

import java.time.Clock;
import java.time.Instant;

@Service
public class BrokerFailedOrderRetryService implements RetryBrokerFailedOrderUseCase {
    private static final Logger log = LoggerFactory.getLogger(BrokerFailedOrderRetryService.class);

    private final OrderRequestPort orderRequestPort;
    private final BrokerPort brokerPort;
    private final TradingSignalPort tradingSignalPort;
    private final OrderStatusHistoryPort orderHistoryPort;
    private final SignalStatusHistoryPort signalHistoryPort;
    private final OperationalMetricsPort operationalMetricsPort;
    private final CorrelationIdProvider correlationIdProvider;
    private final Clock clock;

    @Autowired
    public BrokerFailedOrderRetryService(
            OrderRequestPort orderRequestPort,
            BrokerPort brokerPort,
            TradingSignalPort tradingSignalPort,
            OrderStatusHistoryPort orderHistoryPort,
            SignalStatusHistoryPort signalHistoryPort,
            OperationalMetricsPort operationalMetricsPort,
            CorrelationIdProvider correlationIdProvider
    ) {
        this(
                orderRequestPort,
                brokerPort,
                tradingSignalPort,
                orderHistoryPort,
                signalHistoryPort,
                operationalMetricsPort,
                correlationIdProvider,
                Clock.systemUTC()
        );
    }

    BrokerFailedOrderRetryService(
            OrderRequestPort orderRequestPort,
            BrokerPort brokerPort,
            TradingSignalPort tradingSignalPort,
            Clock clock
    ) {
        this(
                orderRequestPort,
                brokerPort,
                tradingSignalPort,
                OrderStatusHistoryPort.noop(),
                SignalStatusHistoryPort.noop(),
                OperationalMetricsPort.noop(),
                CorrelationIdProvider.generated(),
                clock
        );
    }

    BrokerFailedOrderRetryService(
            OrderRequestPort orderRequestPort,
            BrokerPort brokerPort,
            TradingSignalPort tradingSignalPort,
            OrderStatusHistoryPort orderHistoryPort,
            SignalStatusHistoryPort signalHistoryPort,
            Clock clock
    ) {
        this(
                orderRequestPort,
                brokerPort,
                tradingSignalPort,
                orderHistoryPort,
                signalHistoryPort,
                OperationalMetricsPort.noop(),
                CorrelationIdProvider.generated(),
                clock
        );
    }

    BrokerFailedOrderRetryService(
            OrderRequestPort orderRequestPort,
            BrokerPort brokerPort,
            TradingSignalPort tradingSignalPort,
            OrderStatusHistoryPort orderHistoryPort,
            SignalStatusHistoryPort signalHistoryPort,
            OperationalMetricsPort operationalMetricsPort,
            Clock clock
    ) {
        this(
                orderRequestPort,
                brokerPort,
                tradingSignalPort,
                orderHistoryPort,
                signalHistoryPort,
                operationalMetricsPort,
                CorrelationIdProvider.generated(),
                clock
        );
    }

    BrokerFailedOrderRetryService(
            OrderRequestPort orderRequestPort,
            BrokerPort brokerPort,
            TradingSignalPort tradingSignalPort,
            OrderStatusHistoryPort orderHistoryPort,
            SignalStatusHistoryPort signalHistoryPort,
            OperationalMetricsPort operationalMetricsPort,
            CorrelationIdProvider correlationIdProvider,
            Clock clock
    ) {
        this.orderRequestPort = orderRequestPort;
        this.brokerPort = brokerPort;
        this.tradingSignalPort = tradingSignalPort;
        this.orderHistoryPort = orderHistoryPort;
        this.signalHistoryPort = signalHistoryPort;
        this.operationalMetricsPort = operationalMetricsPort;
        this.correlationIdProvider = correlationIdProvider;
        this.clock = clock;
    }

    @Override
    @Transactional
    public BrokerOrderRetryResult retry(long orderId) {
        try {
            return retryOrder(orderId);
        } catch (OrderRequestNotFoundException | OrderRetryNotAllowedException exception) {
            operationalMetricsPort.recordOrderRetry("rejected");
            log.atWarn()
                    .addKeyValue("orderId", orderId)
                    .addKeyValue("status", "REJECTED")
                    .log("Manual broker retry rejected");
            throw exception;
        }
    }

    private BrokerOrderRetryResult retryOrder(long orderId) {
        String correlationId = correlationIdProvider.currentCorrelationId();
        OrderRequest orderRequest = orderRequestPort.findById(orderId)
                .orElseThrow(() -> new OrderRequestNotFoundException(orderId));
        validateRetry(orderRequest);

        Instant retryRequestedAt = Instant.now(clock);
        if (!orderRequestPort.claimRetry(orderId, retryRequestedAt)) {
            throw new OrderRetryNotAllowedException(
                    "Order retry is already in progress or no longer allowed"
            );
        }
        orderRequest.markRetryRequested(retryRequestedAt);
        orderHistoryPort.save(
                orderId,
                OrderStatus.BROKER_FAILED,
                OrderStatus.RETRY_REQUESTED,
                "Manual broker retry requested",
                correlationIdProvider.currentActor(),
                correlationId,
                retryRequestedAt
        );
        log.atInfo()
                .addKeyValue("orderId", orderId)
                .addKeyValue("signalId", orderRequest.signalId())
                .addKeyValue("status", orderRequest.status())
                .addKeyValue("retryable", orderRequest.retryable())
                .log("Manual broker retry started");

        OrderRequest accepted;
        try {
            accepted = brokerPort.requestOrder(orderRequest);
        } catch (RuntimeException exception) {
            orderRequest.markBrokerFailed(
                    brokerFailureReason(exception),
                    Instant.now(clock),
                    isRetryableBrokerFailure(exception)
            );
            OrderRequest failed = orderRequestPort.updateById(orderId, orderRequest);
            orderHistoryPort.save(
                    orderId,
                    OrderStatus.RETRY_REQUESTED,
                    OrderStatus.BROKER_FAILED,
                    failed.failureReason(),
                    correlationIdProvider.currentActor(),
                    correlationId,
                    failed.failedAt()
            );
            operationalMetricsPort.recordOrderRetry("failed");
            operationalMetricsPort.recordBrokerFailure(failed.retryable());
            log.atWarn()
                    .addKeyValue("orderId", orderId)
                    .addKeyValue("signalId", failed.signalId())
                    .addKeyValue("status", failed.status())
                    .addKeyValue("retryable", failed.retryable())
                    .log("Manual broker retry failed");
            return BrokerOrderRetryResult.brokerFailed(orderId, failed);
        }
        OrderRequest saved = orderRequestPort.updateById(orderId, accepted);
        orderHistoryPort.save(
                orderId,
                OrderStatus.RETRY_REQUESTED,
                OrderStatus.ACCEPTED,
                "Broker accepted manual retry",
                correlationIdProvider.currentActor(),
                correlationId,
                Instant.now(clock)
        );
        synchronizeSignal(saved, correlationId);
        operationalMetricsPort.recordOrderRetry("succeeded");
        log.atInfo()
                .addKeyValue("orderId", orderId)
                .addKeyValue("signalId", saved.signalId())
                .addKeyValue("status", saved.status())
                .addKeyValue("retryable", saved.retryable())
                .log("Manual broker retry succeeded");
        return BrokerOrderRetryResult.accepted(orderId, saved);
    }

    private void synchronizeSignal(OrderRequest orderRequest, String correlationId) {
        if (orderRequest.signalId() == null) {
            log.info(
                    "Skipping TradingSignal synchronization for legacy order without signalId: {}",
                    orderRequest.stockCode()
            );
            return;
        }
        tradingSignalPort.findById(orderRequest.signalId())
                .ifPresentOrElse(signal -> {
                    TradingSignalStatus fromStatus = signal.status();
                    signal.markOrderRequested();
                    tradingSignalPort.save(signal);
                    if (fromStatus != signal.status()) {
                        signalHistoryPort.save(
                                orderRequest.signalId(),
                                fromStatus,
                                signal.status(),
                                "Manual retry accepted by broker",
                                correlationIdProvider.currentActor(),
                                correlationId,
                                Instant.now(clock)
                        );
                    }
                }, () -> log.warn(
                        "TradingSignal not found while synchronizing retried order: signalId={}",
                        orderRequest.signalId()
                ));
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
