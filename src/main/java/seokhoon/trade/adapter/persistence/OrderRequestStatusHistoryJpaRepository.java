package seokhoon.trade.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRequestStatusHistoryJpaRepository
        extends JpaRepository<OrderRequestStatusHistoryEntity, Long> {
    List<OrderRequestStatusHistoryEntity>
    findByOrderRequestIdOrderByCreatedAtAscIdAsc(long orderRequestId);
}
