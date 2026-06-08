package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.order.OrderSide;
import seokhoon.trade.domain.order.OrderStatus;

import java.time.LocalDate;
import java.util.List;

public interface LoadOrderRequestsUseCase {
    List<OrderRequestView> load(String stockCode, LocalDate tradeDate, OrderStatus status, OrderSide side);
}
