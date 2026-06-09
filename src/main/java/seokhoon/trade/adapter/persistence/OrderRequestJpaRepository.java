package seokhoon.trade.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import seokhoon.trade.domain.order.OrderSide;
import seokhoon.trade.domain.order.OrderStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OrderRequestJpaRepository extends JpaRepository<OrderRequestEntity, Long>,
        JpaSpecificationExecutor<OrderRequestEntity> {
    boolean existsByStockCodeAndStrategyNameAndTradeDateAndSide(String stockCode, String strategyName, LocalDate tradeDate, OrderSide side);

    Optional<OrderRequestEntity> findByStockCodeAndStrategyNameAndTradeDateAndSide(
            String stockCode,
            String strategyName,
            LocalDate tradeDate,
            OrderSide side
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update OrderRequestEntity orderRequest
            set orderRequest.status = :retryStatus,
                orderRequest.retryRequestedAt = :retryRequestedAt
            where orderRequest.id = :orderId
              and orderRequest.status = :failedStatus
              and orderRequest.retryable = true
            """)
    int claimRetry(
            @Param("orderId") long orderId,
            @Param("failedStatus") OrderStatus failedStatus,
            @Param("retryStatus") OrderStatus retryStatus,
            @Param("retryRequestedAt") Instant retryRequestedAt
    );

    List<OrderRequestEntity> findByStatusAndRetryRequestedAtLessThanEqualOrderByRetryRequestedAtAsc(
            OrderStatus status,
            Instant cutoff
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update OrderRequestEntity orderRequest
            set orderRequest.status = :failedStatus,
                orderRequest.brokerOrderNo = null,
                orderRequest.failureReason = :failureReason,
                orderRequest.failedAt = :failedAt,
                orderRequest.retryable = true,
                orderRequest.retryRequestedAt = null
            where orderRequest.id = :orderId
              and orderRequest.status = :retryStatus
              and orderRequest.retryRequestedAt <= :cutoff
            """)
    int recoverStuckRetry(
            @Param("orderId") long orderId,
            @Param("retryStatus") OrderStatus retryStatus,
            @Param("failedStatus") OrderStatus failedStatus,
            @Param("cutoff") Instant cutoff,
            @Param("failureReason") String failureReason,
            @Param("failedAt") Instant failedAt
    );
}
