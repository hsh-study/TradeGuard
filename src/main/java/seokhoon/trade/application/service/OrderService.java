package seokhoon.trade.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.in.RequestMockOrderUseCase;
import seokhoon.trade.application.port.out.BrokerPort;
import seokhoon.trade.application.port.out.OrderRequestPort;
import seokhoon.trade.application.port.out.TradingSignalPort;
import seokhoon.trade.domain.order.OrderRequest;
import seokhoon.trade.domain.order.OrderSide;
import seokhoon.trade.domain.order.OrderType;
import seokhoon.trade.domain.risk.RiskDecision;
import seokhoon.trade.domain.risk.RiskManager;
import seokhoon.trade.domain.strategy.TradingSignal;

import java.math.BigDecimal;

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
    @Transactional
    public OrderRequest request(TradingSignal signal, int quantity, BigDecimal limitPrice) {
        OrderRequest orderRequest = new OrderRequest(signal.stockCode(), OrderSide.BUY, OrderType.LIMIT, quantity, limitPrice,
                signal.strategyName(), signal.signalDate());
        RiskDecision decision = riskManager.evaluate(signal, orderRequest, orderRequestPort::exists);
        tradingSignalPort.save(signal);
        if (!decision.approved()) {
            throw new IllegalStateException("Risk rejected: " + String.join(",", decision.reasons()));
        }
        OrderRequest requested = brokerPort.requestOrder(orderRequest);
        signal.markOrderRequested();
        tradingSignalPort.save(signal);
        return orderRequestPort.save(requested);
    }
}
