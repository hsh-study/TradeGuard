package seokhoon.trade.application.service;

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
import java.util.List;

@Service
public class OrderService implements RequestMockOrderUseCase {
    private final OrderRequestPort orderRequestPort;
    private final TradingSignalPort tradingSignalPort;
    private final BrokerPort brokerPort;
    private final RiskManager riskManager;

    public OrderService(OrderRequestPort orderRequestPort, TradingSignalPort tradingSignalPort, BrokerPort brokerPort, RiskManager riskManager) {
        this.orderRequestPort = orderRequestPort;
        this.tradingSignalPort = tradingSignalPort;
        this.brokerPort = brokerPort;
        this.riskManager = riskManager;
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

        OrderRequest requested = brokerPort.requestOrder(orderRequest);
        signal.markOrderRequested();
        tradingSignalPort.save(signal);
        return MockOrderResult.accepted(decision, signal, orderRequestPort.update(requested));
    }
}
