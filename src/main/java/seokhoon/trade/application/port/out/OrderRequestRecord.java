package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.order.OrderSide;
import seokhoon.trade.domain.order.OrderStatus;
import seokhoon.trade.domain.order.OrderType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OrderRequestRecord(
        Long id,
        String stockCode,
        OrderSide side,
        OrderType orderType,
        int quantity,
        BigDecimal limitPrice,
        OrderStatus status,
        String brokerOrderNo,
        String strategyName,
        LocalDate tradeDate
) {
}
