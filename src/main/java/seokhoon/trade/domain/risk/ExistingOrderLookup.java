package seokhoon.trade.domain.risk;

import seokhoon.trade.domain.order.OrderSide;

import java.time.LocalDate;

public interface ExistingOrderLookup {
    boolean exists(String stockCode, String strategyName, LocalDate tradeDate, OrderSide side);
}
