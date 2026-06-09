package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.order.OrderStatus;

import java.time.Instant;
import java.util.List;

public interface OrderStatusHistoryPort {
    void save(
            long orderRequestId,
            OrderStatus fromStatus,
            OrderStatus toStatus,
            String reason,
            Instant createdAt
    );

    List<OrderStatusHistoryRecord> findByOrderRequestId(long orderRequestId);

    static OrderStatusHistoryPort noop() {
        return new OrderStatusHistoryPort() {
            @Override
            public void save(
                    long orderRequestId,
                    OrderStatus fromStatus,
                    OrderStatus toStatus,
                    String reason,
                    Instant createdAt
            ) {
            }

            @Override
            public List<OrderStatusHistoryRecord> findByOrderRequestId(long orderRequestId) {
                return List.of();
            }
        };
    }
}
