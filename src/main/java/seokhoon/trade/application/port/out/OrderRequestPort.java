package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.order.OrderRequest;
import seokhoon.trade.domain.order.OrderSide;
import seokhoon.trade.domain.order.OrderStatus;

import java.time.LocalDate;
import java.util.List;

public interface OrderRequestPort {
    OrderRequest create(OrderRequest orderRequest);
    OrderRequest update(OrderRequest orderRequest);
    boolean exists(String stockCode, String strategyName, LocalDate tradeDate, OrderSide side);
    List<OrderRequestRecord> find(String stockCode, LocalDate tradeDate, OrderStatus status, OrderSide side);
}
