package seokhoon.trade.adapter.persistence;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.out.DuplicateOrderRequestException;
import seokhoon.trade.application.port.out.OrderRequestPort;
import seokhoon.trade.application.port.out.OrderRequestRecord;
import seokhoon.trade.domain.order.OrderRequest;
import seokhoon.trade.domain.order.OrderSide;
import seokhoon.trade.domain.order.OrderStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
    public OrderRequest updateById(long orderId, OrderRequest orderRequest) {
        OrderRequestEntity entity = repository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("Order request not found: " + orderId));
        entity.update(orderRequest);
        repository.saveAndFlush(entity);
        return orderRequest;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderRequest> findById(long orderId) {
        return repository.findById(orderId).map(OrderRequestEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> findId(OrderRequest orderRequest) {
        return repository.findByStockCodeAndStrategyNameAndTradeDateAndSide(
                        orderRequest.stockCode(),
                        orderRequest.strategyName(),
                        orderRequest.tradeDate(),
                        orderRequest.side()
                )
                .map(OrderRequestEntity::id);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claimRetry(long orderId) {
        return claimRetry(orderId, Instant.now());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claimRetry(long orderId, Instant retryRequestedAt) {
        return repository.claimRetry(
                orderId,
                OrderStatus.BROKER_FAILED,
                OrderStatus.RETRY_REQUESTED,
                retryRequestedAt
        ) == 1;
    }

    @Override
    public boolean exists(String stockCode, String strategyName, LocalDate tradeDate, OrderSide side) {
        return repository.existsByStockCodeAndStrategyNameAndTradeDateAndSide(stockCode, strategyName, tradeDate, side);
    }

    @Override
    public List<OrderRequestRecord> find(String stockCode, LocalDate tradeDate, OrderStatus status, OrderSide side) {
        return find(stockCode, tradeDate, status, side, null);
    }

    @Override
    public List<OrderRequestRecord> find(
            String stockCode,
            LocalDate tradeDate,
            OrderStatus status,
            OrderSide side,
            Long signalId
    ) {
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
        if (side != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("side"), side));
        }
        if (signalId != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("signalId"), signalId));
        }
        return repository.findAll(
                        specification,
                        Sort.by(Sort.Order.desc("tradeDate"), Sort.Order.desc("id"))
                )
                .stream()
                .map(OrderRequestEntity::toRecord)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderRequestRecord> findStuckRetries(Instant cutoff) {
        return repository
                .findByStatusAndRetryRequestedAtLessThanEqualOrderByRetryRequestedAtAsc(
                        OrderStatus.RETRY_REQUESTED,
                        cutoff
                )
                .stream()
                .map(OrderRequestEntity::toRecord)
                .toList();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean recoverStuckRetry(
            long orderId,
            Instant cutoff,
            OrderRequest recoveredOrder
    ) {
        return repository.recoverStuckRetry(
                orderId,
                OrderStatus.RETRY_REQUESTED,
                OrderStatus.BROKER_FAILED,
                cutoff,
                recoveredOrder.failureReason(),
                recoveredOrder.failedAt()
        ) == 1;
    }
}
