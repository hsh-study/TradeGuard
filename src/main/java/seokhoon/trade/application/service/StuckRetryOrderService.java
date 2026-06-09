package seokhoon.trade.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.in.LoadStuckRetryOrdersUseCase;
import seokhoon.trade.application.port.in.OrderRequestView;
import seokhoon.trade.application.port.in.RecoverStuckRetryOrderUseCase;
import seokhoon.trade.application.port.out.OrderRequestPort;
import seokhoon.trade.application.port.out.OrderRequestRecord;
import seokhoon.trade.application.port.out.OrderStatusHistoryPort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.domain.order.OrderRequest;
import seokhoon.trade.domain.order.OrderStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class StuckRetryOrderService
        implements LoadStuckRetryOrdersUseCase, RecoverStuckRetryOrderUseCase {
    private static final Logger log = LoggerFactory.getLogger(StuckRetryOrderService.class);

    private final OrderRequestPort orderRequestPort;
    private final OrderStatusHistoryPort orderHistoryPort;
    private final OperationalMetricsPort operationalMetricsPort;

    @Autowired
    public StuckRetryOrderService(
            OrderRequestPort orderRequestPort,
            OrderStatusHistoryPort orderHistoryPort,
            OperationalMetricsPort operationalMetricsPort
    ) {
        this.orderRequestPort = orderRequestPort;
        this.orderHistoryPort = orderHistoryPort;
        this.operationalMetricsPort = operationalMetricsPort;
    }

    StuckRetryOrderService(OrderRequestPort orderRequestPort) {
        this(
                orderRequestPort,
                OrderStatusHistoryPort.noop(),
                OperationalMetricsPort.noop()
        );
    }

    StuckRetryOrderService(
            OrderRequestPort orderRequestPort,
            OrderStatusHistoryPort orderHistoryPort
    ) {
        this(orderRequestPort, orderHistoryPort, OperationalMetricsPort.noop());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderRequestView> load(Instant referenceTime, Duration threshold) {
        Instant cutoff = cutoff(referenceTime, threshold);
        return orderRequestPort.findStuckRetries(cutoff).stream()
                .map(StuckRetryOrderService::toView)
                .toList();
    }

    @Override
    @Transactional
    public OrderRequestView recover(
            long orderId,
            String reason,
            Instant referenceTime,
            Duration threshold
    ) {
        try {
            return recoverOrder(orderId, reason, referenceTime, threshold);
        } catch (OrderRequestNotFoundException | StuckRetryRecoveryNotAllowedException exception) {
            operationalMetricsPort.recordOrderRetryRecovery("rejected");
            log.atWarn()
                    .addKeyValue("orderId", orderId)
                    .addKeyValue("status", "REJECTED")
                    .log("Stuck retry recovery rejected");
            throw exception;
        }
    }

    private OrderRequestView recoverOrder(
            long orderId,
            String reason,
            Instant referenceTime,
            Duration threshold
    ) {
        Instant cutoff = cutoff(referenceTime, threshold);
        OrderRequest orderRequest = orderRequestPort.findById(orderId)
                .orElseThrow(() -> new OrderRequestNotFoundException(orderId));
        if (orderRequest.status() != OrderStatus.RETRY_REQUESTED) {
            throw new StuckRetryRecoveryNotAllowedException(
                    "Only RETRY_REQUESTED orders can be recovered"
            );
        }
        if (orderRequest.retryRequestedAt() == null
                || orderRequest.retryRequestedAt().isAfter(cutoff)) {
            throw new StuckRetryRecoveryNotAllowedException(
                    "Order retry has not exceeded the stuck threshold"
            );
        }
        orderRequest.markRetryStuckRecovered(reason, referenceTime);
        if (!orderRequestPort.recoverStuckRetry(orderId, cutoff, orderRequest)) {
            throw new StuckRetryRecoveryNotAllowedException(
                    "Order retry is no longer recoverable"
            );
        }
        orderHistoryPort.save(
                orderId,
                OrderStatus.RETRY_REQUESTED,
                OrderStatus.BROKER_FAILED,
                orderRequest.failureReason(),
                referenceTime
        );
        operationalMetricsPort.recordOrderRetryRecovery("succeeded");
        log.atInfo()
                .addKeyValue("orderId", orderId)
                .addKeyValue("signalId", orderRequest.signalId())
                .addKeyValue("status", orderRequest.status())
                .addKeyValue("retryable", orderRequest.retryable())
                .log("Stuck retry recovered");
        return toView(orderId, orderRequest);
    }

    private static Instant cutoff(Instant referenceTime, Duration threshold) {
        if (referenceTime == null) {
            throw new IllegalArgumentException("referenceTime must not be null");
        }
        if (threshold == null || threshold.isZero() || threshold.isNegative()) {
            throw new IllegalArgumentException("threshold must be positive");
        }
        return referenceTime.minus(threshold);
    }

    private static OrderRequestView toView(OrderRequestRecord record) {
        return new OrderRequestView(
                record.id(),
                record.stockCode(),
                record.side(),
                record.orderType(),
                record.quantity(),
                record.limitPrice(),
                record.status(),
                record.brokerOrderNo(),
                record.failureReason(),
                record.failedAt(),
                record.retryable(),
                record.strategyName(),
                record.tradeDate(),
                record.signalId(),
                record.retryRequestedAt()
        );
    }

    private static OrderRequestView toView(long orderId, OrderRequest orderRequest) {
        return new OrderRequestView(
                orderId,
                orderRequest.stockCode(),
                orderRequest.side(),
                orderRequest.orderType(),
                orderRequest.quantity(),
                orderRequest.limitPrice(),
                orderRequest.status(),
                orderRequest.brokerOrderNo(),
                orderRequest.failureReason(),
                orderRequest.failedAt(),
                orderRequest.retryable(),
                orderRequest.strategyName(),
                orderRequest.tradeDate(),
                orderRequest.signalId(),
                orderRequest.retryRequestedAt()
        );
    }
}
