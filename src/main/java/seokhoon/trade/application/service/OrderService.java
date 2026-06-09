package seokhoon.trade.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.in.MockOrderResult;
import seokhoon.trade.application.port.in.RequestMockOrderUseCase;
import seokhoon.trade.application.port.out.BrokerPort;
import seokhoon.trade.application.port.out.CorrelationIdProvider;
import seokhoon.trade.application.port.out.DuplicateOrderRequestException;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.application.port.out.OrderRequestPort;
import seokhoon.trade.application.port.out.OrderStatusHistoryPort;
import seokhoon.trade.application.port.out.SignalStatusHistoryPort;
import seokhoon.trade.application.port.out.TradingSignalPort;
import seokhoon.trade.domain.order.OrderRequest;
import seokhoon.trade.domain.order.OrderSide;
import seokhoon.trade.domain.order.OrderType;
import seokhoon.trade.domain.order.OrderStatus;
import seokhoon.trade.domain.risk.RiskDecision;
import seokhoon.trade.domain.risk.RiskManager;
import seokhoon.trade.domain.strategy.TradingSignal;
import seokhoon.trade.domain.strategy.TradingSignalStatus;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class OrderService implements RequestMockOrderUseCase {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRequestPort orderRequestPort;
    private final TradingSignalPort tradingSignalPort;
    private final BrokerPort brokerPort;
    private final RiskManager riskManager;
    private final SignalStatusHistoryPort signalHistoryPort;
    private final OrderStatusHistoryPort orderHistoryPort;
    private final OperationalMetricsPort operationalMetricsPort;
    private final CorrelationIdProvider correlationIdProvider;
    private final Clock clock;

    @Autowired
    public OrderService(
            OrderRequestPort orderRequestPort,
            TradingSignalPort tradingSignalPort,
            BrokerPort brokerPort,
            RiskManager riskManager,
            SignalStatusHistoryPort signalHistoryPort,
            OrderStatusHistoryPort orderHistoryPort,
            OperationalMetricsPort operationalMetricsPort,
            CorrelationIdProvider correlationIdProvider
    ) {
        this(
                orderRequestPort,
                tradingSignalPort,
                brokerPort,
                riskManager,
                signalHistoryPort,
                orderHistoryPort,
                operationalMetricsPort,
                correlationIdProvider,
                Clock.systemUTC()
        );
    }

    OrderService(
            OrderRequestPort orderRequestPort,
            TradingSignalPort tradingSignalPort,
            BrokerPort brokerPort,
            RiskManager riskManager
    ) {
        this(
                orderRequestPort,
                tradingSignalPort,
                brokerPort,
                riskManager,
                SignalStatusHistoryPort.noop(),
                OrderStatusHistoryPort.noop(),
                OperationalMetricsPort.noop(),
                CorrelationIdProvider.generated(),
                Clock.systemUTC()
        );
    }

    OrderService(
            OrderRequestPort orderRequestPort,
            TradingSignalPort tradingSignalPort,
            BrokerPort brokerPort,
            RiskManager riskManager,
            Clock clock
    ) {
        this(
                orderRequestPort,
                tradingSignalPort,
                brokerPort,
                riskManager,
                SignalStatusHistoryPort.noop(),
                OrderStatusHistoryPort.noop(),
                OperationalMetricsPort.noop(),
                CorrelationIdProvider.generated(),
                clock
        );
    }

    OrderService(
            OrderRequestPort orderRequestPort,
            TradingSignalPort tradingSignalPort,
            BrokerPort brokerPort,
            RiskManager riskManager,
            SignalStatusHistoryPort signalHistoryPort,
            OrderStatusHistoryPort orderHistoryPort,
            Clock clock
    ) {
        this(
                orderRequestPort,
                tradingSignalPort,
                brokerPort,
                riskManager,
                signalHistoryPort,
                orderHistoryPort,
                OperationalMetricsPort.noop(),
                CorrelationIdProvider.generated(),
                clock
        );
    }

    OrderService(
            OrderRequestPort orderRequestPort,
            TradingSignalPort tradingSignalPort,
            BrokerPort brokerPort,
            RiskManager riskManager,
            SignalStatusHistoryPort signalHistoryPort,
            OrderStatusHistoryPort orderHistoryPort,
            OperationalMetricsPort operationalMetricsPort,
            Clock clock
    ) {
        this(
                orderRequestPort,
                tradingSignalPort,
                brokerPort,
                riskManager,
                signalHistoryPort,
                orderHistoryPort,
                operationalMetricsPort,
                CorrelationIdProvider.generated(),
                clock
        );
    }

    OrderService(
            OrderRequestPort orderRequestPort,
            TradingSignalPort tradingSignalPort,
            BrokerPort brokerPort,
            RiskManager riskManager,
            SignalStatusHistoryPort signalHistoryPort,
            OrderStatusHistoryPort orderHistoryPort,
            OperationalMetricsPort operationalMetricsPort,
            CorrelationIdProvider correlationIdProvider,
            Clock clock
    ) {
        this.orderRequestPort = orderRequestPort;
        this.tradingSignalPort = tradingSignalPort;
        this.brokerPort = brokerPort;
        this.riskManager = riskManager;
        this.signalHistoryPort = signalHistoryPort;
        this.orderHistoryPort = orderHistoryPort;
        this.operationalMetricsPort = operationalMetricsPort;
        this.correlationIdProvider = correlationIdProvider;
        this.clock = clock;
    }

    @Override
    public MockOrderResult request(TradingSignal signal, int quantity, BigDecimal limitPrice) {
        return request(signal, null, quantity, limitPrice);
    }

    @Override
    @Transactional
    public MockOrderResult request(
            TradingSignal signal,
            Long signalId,
            int quantity,
            BigDecimal limitPrice
    ) {
        String correlationId = correlationIdProvider.currentCorrelationId();
        OrderRequest orderRequest = new OrderRequest(
                signal.stockCode(),
                OrderSide.BUY,
                OrderType.LIMIT,
                quantity,
                limitPrice,
                signal.strategyName(),
                signal.signalDate(),
                signalId
        );
        TradingSignalStatus signalFromStatus = signal.status();
        RiskDecision decision = riskManager.evaluate(signal, orderRequest, orderRequestPort::exists);
        tradingSignalPort.save(signal);
        Long effectiveSignalId = signalId != null
                ? signalId
                : tradingSignalPort.findId(
                        signal.strategyName(),
                        signal.stockCode(),
                        signal.signalDate(),
                        signal.signalType()
                ).orElse(null);
        if (decision.approved()) {
            saveSignalHistory(
                    effectiveSignalId,
                    signalFromStatus,
                    signal.status(),
                    "Risk policy approved",
                    correlationId
            );
        }
        if (!decision.approved()) {
            operationalMetricsPort.recordOrderRequest("RISK_REJECTED");
            return MockOrderResult.rejected(decision, signal);
        }

        try {
            orderRequestPort.create(orderRequest);
        } catch (DuplicateOrderRequestException exception) {
            RiskDecision duplicateDecision = RiskDecision.rejected(List.of("DUPLICATE_ORDER"));
            signal.rejectRisk(duplicateDecision.reasons());
            tradingSignalPort.save(signal);
            operationalMetricsPort.recordOrderRequest("DUPLICATE_ORDER");
            return MockOrderResult.rejected(duplicateDecision, signal);
        }
        Long orderId = orderRequestPort.findId(orderRequest).orElse(null);

        OrderRequest requested;
        try {
            requested = brokerPort.requestOrder(orderRequest);
        } catch (RuntimeException exception) {
            String failureReason = brokerFailureReason(exception);
            orderRequest.markBrokerFailed(
                    failureReason,
                    Instant.now(clock),
                    isRetryableBrokerFailure(exception)
            );
            OrderRequest failedOrder = orderRequestPort.update(orderRequest);
            saveOrderHistory(
                    orderId,
                    OrderStatus.CREATED,
                    OrderStatus.BROKER_FAILED,
                    failureReason,
                    correlationId
            );
            operationalMetricsPort.recordOrderRequest(OrderStatus.BROKER_FAILED.name());
            operationalMetricsPort.recordBrokerFailure(failedOrder.retryable());
            log.atWarn()
                    .addKeyValue("orderId", orderId)
                    .addKeyValue("signalId", effectiveSignalId)
                    .addKeyValue("status", failedOrder.status())
                    .addKeyValue("retryable", failedOrder.retryable())
                    .log("Broker order request failed");
            return MockOrderResult.brokerFailed(decision, signal, failedOrder);
        }
        OrderRequest acceptedOrder = orderRequestPort.update(requested);
        saveOrderHistory(
                orderId,
                OrderStatus.CREATED,
                acceptedOrder.status(),
                "Broker accepted mock order",
                correlationId
        );
        TradingSignalStatus approvedStatus = signal.status();
        signal.markOrderRequested();
        tradingSignalPort.save(signal);
        saveSignalHistory(
                effectiveSignalId,
                approvedStatus,
                signal.status(),
                "Mock order accepted by broker",
                correlationId
        );
        operationalMetricsPort.recordOrderRequest(acceptedOrder.status().name());
        log.atInfo()
                .addKeyValue("orderId", orderId)
                .addKeyValue("signalId", effectiveSignalId)
                .addKeyValue("status", acceptedOrder.status())
                .addKeyValue("retryable", acceptedOrder.retryable())
                .log("Broker order request accepted");
        return MockOrderResult.accepted(decision, signal, acceptedOrder);
    }

    private void saveSignalHistory(
            Long signalId,
            TradingSignalStatus fromStatus,
            TradingSignalStatus toStatus,
            String reason,
            String correlationId
    ) {
        if (signalId != null && fromStatus != toStatus) {
            signalHistoryPort.save(
                    signalId,
                    fromStatus,
                    toStatus,
                    reason,
                    correlationIdProvider.currentActor(),
                    correlationId,
                    Instant.now(clock)
            );
        }
    }

    private void saveOrderHistory(
            Long orderId,
            OrderStatus fromStatus,
            OrderStatus toStatus,
            String reason,
            String correlationId
    ) {
        if (orderId != null && fromStatus != toStatus) {
            orderHistoryPort.save(
                    orderId,
                    fromStatus,
                    toStatus,
                    reason,
                    correlationIdProvider.currentActor(),
                    correlationId,
                    Instant.now(clock)
            );
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
