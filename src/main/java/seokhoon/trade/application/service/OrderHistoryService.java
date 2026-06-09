package seokhoon.trade.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.in.LoadOrderRequestsUseCase;
import seokhoon.trade.application.port.in.OrderRequestView;
import seokhoon.trade.application.port.out.OrderRequestPort;
import seokhoon.trade.application.port.out.OrderRequestRecord;
import seokhoon.trade.domain.order.OrderSide;
import seokhoon.trade.domain.order.OrderStatus;

import java.time.LocalDate;
import java.util.List;

@Service
public class OrderHistoryService implements LoadOrderRequestsUseCase {
    private final OrderRequestPort orderRequestPort;

    public OrderHistoryService(OrderRequestPort orderRequestPort) {
        this.orderRequestPort = orderRequestPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderRequestView> load(String stockCode, LocalDate tradeDate, OrderStatus status, OrderSide side) {
        return load(stockCode, tradeDate, status, side, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderRequestView> load(
            String stockCode,
            LocalDate tradeDate,
            OrderStatus status,
            OrderSide side,
            Long signalId
    ) {
        if (stockCode != null && stockCode.isBlank()) {
            throw new IllegalArgumentException("stockCode must not be blank");
        }
        return orderRequestPort.find(stockCode, tradeDate, status, side, signalId).stream()
                .map(OrderHistoryService::toView)
                .toList();
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
                record.signalId()
        );
    }
}
