package seokhoon.trade.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import seokhoon.trade.domain.order.OrderSide;

import java.time.LocalDate;

public interface OrderRequestJpaRepository extends JpaRepository<OrderRequestEntity, Long> {
    boolean existsByStockCodeAndStrategyNameAndTradeDateAndSide(String stockCode, String strategyName, LocalDate tradeDate, OrderSide side);
}
