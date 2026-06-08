package seokhoon.trade.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import seokhoon.trade.application.port.in.MockOrderResult;
import seokhoon.trade.application.port.in.RequestMockOrderUseCase;
import seokhoon.trade.application.port.out.BrokerPort;
import seokhoon.trade.application.port.out.DuplicateOrderRequestException;
import seokhoon.trade.application.port.out.OrderRequestPort;
import seokhoon.trade.application.port.out.TradingSignalPort;
import seokhoon.trade.domain.order.OrderRequest;
import seokhoon.trade.domain.order.OrderSide;
import seokhoon.trade.domain.order.OrderType;
import seokhoon.trade.domain.risk.RiskDecision;
import seokhoon.trade.domain.risk.RiskManager;
import seokhoon.trade.domain.strategy.TradingSignal;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class OrderService implements RequestMockOrderUseCase {
    private final OrderRequestPort orderRequestPort;
    private final TradingSignalPort tradingSignalPort;
    private final BrokerPort brokerPort;
    private final RiskManager riskManager;
    private final Clock clock;

    @Autowired
    public OrderService(
            OrderRequestPort orderRequestPort,
            TradingSignalPort tradingSignalPort,
            BrokerPort brokerPort,
            RiskManager riskManager
    ) {
        this(orderRequestPort, tradingSignalPort, brokerPort, riskManager, Clock.systemUTC());
    }

    OrderService(
            OrderRequestPort orderRequestPort,
            TradingSignalPort tradingSignalPort,
            BrokerPort brokerPort,
            RiskManager riskManager,
            Clock clock
    ) {
        this.orderRequestPort = orderRequestPort;
        this.tradingSignalPort = tradingSignalPort;
        this.brokerPort = brokerPort;
        this.riskManager = riskManager;
        this.clock = clock;
    }

    @Override
    public MockOrderResult request(TradingSignal signal, int quantity, BigDecimal limitPrice) {
        OrderRequest orderRequest = new OrderRequest(signal.stockCode(), OrderSide.BUY, OrderType.LIMIT, quantity, limitPrice,
                signal.strategyName(), signal.signalDate());
        RiskDecision decision = riskManager.evaluate(signal, orderRequest, orderRequestPort::exists);
        tradingSignalPort.save(signal);
        if (!decision.approved()) {
            return MockOrderResult.rejected(decision, signal);
        }

        try {
            orderRequestPort.create(orderRequest);
        } catch (DuplicateOrderRequestException exception) {
            RiskDecision duplicateDecision = RiskDecision.rejected(List.of("DUPLICATE_ORDER"));
            signal.rejectRisk(duplicateDecision.reasons());
            tradingSignalPort.save(signal);
            return MockOrderResult.rejected(duplicateDecision, signal);
        }

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
            return MockOrderResult.brokerFailed(decision, signal, failedOrder);
        }
        signal.markOrderRequested();
        tradingSignalPort.save(signal);
        return MockOrderResult.accepted(decision, signal, orderRequestPort.update(requested));
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
