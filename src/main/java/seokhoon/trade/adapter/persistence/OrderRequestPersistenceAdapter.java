package seokhoon.trade.adapter.persistence;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.out.DuplicateOrderRequestException;
import seokhoon.trade.application.port.out.OrderRequestPort;
import seokhoon.trade.domain.order.OrderRequest;
import seokhoon.trade.domain.order.OrderSide;
import seokhoon.trade.domain.order.OrderStatus;

import java.time.LocalDate;
import java.util.List;

@Component
public class OrderRequestPersistenceAdapter implements OrderRequestPort {
    private final OrderRequestJpaRepository repository;

    public OrderRequestPersistenceAdapter(OrderRequestJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public OrderRequest create(OrderRequest orderRequest) {
        try {
            repository.saveAndFlush(OrderRequestEntity.from(orderRequest));
            return orderRequest;
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateOrderRequestException(exception);
        }
    }

    @Override
    public OrderRequest update(OrderRequest orderRequest) {
        OrderRequestEntity entity = repository.findByStockCodeAndStrategyNameAndTradeDateAndSide(
                        orderRequest.stockCode(),
                        orderRequest.strategyName(),
                        orderRequest.tradeDate(),
                        orderRequest.side()
                )
                .orElseThrow(() -> new IllegalStateException("Order request reservation not found"));
        entity.update(orderRequest);
        repository.save(entity);
        return orderRequest;
    }

    @Override
    public boolean exists(String stockCode, String strategyName, LocalDate tradeDate, OrderSide side) {
        return repository.existsByStockCodeAndStrategyNameAndTradeDateAndSide(stockCode, strategyName, tradeDate, side);
    }

    @Override
    public List<OrderRequest> find(String stockCode, LocalDate tradeDate, OrderStatus status) {
        Specification<OrderRequestEntity> specification = (root, query, criteriaBuilder) ->
                criteriaBuilder.conjunction();
        if (stockCode != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("stockCode"), stockCode));
        }
        if (tradeDate != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("tradeDate"), tradeDate));
        }
        if (status != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("status"), status));
        }
        return repository.findAll(
                        specification,
                        Sort.by(Sort.Order.desc("tradeDate"), Sort.Order.desc("id"))
                )
                .stream()
                .map(OrderRequestEntity::toDomain)
                .toList();
    }
}
