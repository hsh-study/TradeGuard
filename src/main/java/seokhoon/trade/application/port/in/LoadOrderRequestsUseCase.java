package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.order.OrderRequest;
import seokhoon.trade.domain.order.OrderStatus;

import java.time.LocalDate;
import java.util.List;

public interface LoadOrderRequestsUseCase {
    List<OrderRequest> load(String stockCode, LocalDate tradeDate, OrderStatus status);
}
