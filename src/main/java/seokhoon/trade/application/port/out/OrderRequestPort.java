package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.order.OrderRequest;
import seokhoon.trade.domain.order.OrderSide;
import seokhoon.trade.domain.order.OrderStatus;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRequestPort {
    OrderRequest create(OrderRequest orderRequest);
    OrderRequest update(OrderRequest orderRequest);
    OrderRequest updateById(long orderId, OrderRequest orderRequest);
    Optional<OrderRequest> findById(long orderId);
    boolean claimRetry(long orderId);
    default boolean claimRetry(long orderId, Instant retryRequestedAt) {
        return claimRetry(orderId);
    }
    boolean exists(String stockCode, String strategyName, LocalDate tradeDate, OrderSide side);
    List<OrderRequestRecord> find(String stockCode, LocalDate tradeDate, OrderStatus status, OrderSide side);

    default List<OrderRequestRecord> find(
            String stockCode,
            LocalDate tradeDate,
            OrderStatus status,
            OrderSide side,
            Long signalId
    ) {
        return find(stockCode, tradeDate, status, side);
    }

    default List<OrderRequestRecord> findStuckRetries(Instant cutoff) {
        return List.of();
    }

    default boolean recoverStuckRetry(
            long orderId,
            Instant cutoff,
            OrderRequest recoveredOrder
    ) {
        updateById(orderId, recoveredOrder);
        return true;
    }
}
