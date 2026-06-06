package seokhoon.trade.adapter.persistence;

import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.out.OrderRequestPort;
import seokhoon.trade.domain.order.OrderRequest;
import seokhoon.trade.domain.order.OrderSide;

import java.time.LocalDate;

@Component
public class OrderRequestPersistenceAdapter implements OrderRequestPort {
    private final OrderRequestJpaRepository repository;

    public OrderRequestPersistenceAdapter(OrderRequestJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public OrderRequest save(OrderRequest orderRequest) {
        repository.save(OrderRequestEntity.from(orderRequest));
        return orderRequest;
    }

    @Override
    public boolean exists(String stockCode, String strategyName, LocalDate tradeDate, OrderSide side) {
        return repository.existsByStockCodeAndStrategyNameAndTradeDateAndSide(stockCode, strategyName, tradeDate, side);
    }
}
