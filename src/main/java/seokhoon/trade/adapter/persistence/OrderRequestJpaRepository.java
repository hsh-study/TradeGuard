package seokhoon.trade.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import seokhoon.trade.domain.order.OrderSide;
import seokhoon.trade.domain.order.OrderStatus;

import java.time.LocalDate;
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
            set orderRequest.status = :retryStatus
            where orderRequest.id = :orderId
              and orderRequest.status = :failedStatus
              and orderRequest.retryable = true
            """)
    int claimRetry(
            @Param("orderId") long orderId,
            @Param("failedStatus") OrderStatus failedStatus,
            @Param("retryStatus") OrderStatus retryStatus
    );
}
