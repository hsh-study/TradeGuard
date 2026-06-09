package seokhoon.trade.adapter.persistence;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.out.OrderStatusHistoryPort;
import seokhoon.trade.application.port.out.OrderStatusHistoryRecord;
import seokhoon.trade.domain.order.OrderStatus;
import seokhoon.trade.domain.audit.AuditActor;

import java.time.Instant;
import java.util.List;

@Component
public class OrderStatusHistoryPersistenceAdapter implements OrderStatusHistoryPort {
    private final OrderRequestStatusHistoryJpaRepository repository;

    public OrderStatusHistoryPersistenceAdapter(
            OrderRequestStatusHistoryJpaRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    public void save(
            long orderRequestId,
            OrderStatus fromStatus,
            OrderStatus toStatus,
            String reason,
            AuditActor actor,
            String requestCorrelationId,
            Instant createdAt
    ) {
        repository.save(new OrderRequestStatusHistoryEntity(
                orderRequestId,
                fromStatus,
                toStatus,
                reason,
                actor,
                requestCorrelationId,
                createdAt
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderStatusHistoryRecord> findByOrderRequestId(long orderRequestId) {
        return repository.findByOrderRequestIdOrderByCreatedAtAscIdAsc(orderRequestId)
                .stream()
                .map(OrderRequestStatusHistoryEntity::toRecord)
                .toList();
    }
}
