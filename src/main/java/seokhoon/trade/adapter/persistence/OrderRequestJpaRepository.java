package seokhoon.trade.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import seokhoon.trade.domain.order.OrderSide;

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
}
