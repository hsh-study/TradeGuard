package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.order.OrderRequest;
import seokhoon.trade.domain.order.OrderSide;

import java.time.LocalDate;

public interface OrderRequestPort {
    OrderRequest save(OrderRequest orderRequest);
    boolean exists(String stockCode, String strategyName, LocalDate tradeDate, OrderSide side);
}
