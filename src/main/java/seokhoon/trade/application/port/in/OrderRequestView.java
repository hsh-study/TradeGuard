package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.order.OrderSide;
import seokhoon.trade.domain.order.OrderStatus;
import seokhoon.trade.domain.order.OrderType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record OrderRequestView(
        Long orderId,
        String stockCode,
        OrderSide side,
        OrderType orderType,
        int quantity,
        BigDecimal limitPrice,
        OrderStatus status,
        String brokerOrderNo,
        String failureReason,
        Instant failedAt,
        boolean retryable,
        String strategyName,
        LocalDate tradeDate
) {
}
