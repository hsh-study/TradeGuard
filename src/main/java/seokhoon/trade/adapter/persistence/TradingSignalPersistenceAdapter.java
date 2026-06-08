package seokhoon.trade.adapter.persistence;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.in.TradingSignalSearchCriteria;
import seokhoon.trade.application.port.out.TradingSignalQueryPort;
import seokhoon.trade.application.port.out.TradingSignalRecord;
import seokhoon.trade.application.port.out.TradingSignalPort;
import seokhoon.trade.domain.strategy.SignalType;
import seokhoon.trade.domain.strategy.TradingSignal;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class TradingSignalPersistenceAdapter implements TradingSignalPort, TradingSignalQueryPort {
    private final TradingSignalJpaRepository repository;

    public TradingSignalPersistenceAdapter(TradingSignalJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public TradingSignal save(TradingSignal tradingSignal) {
        TradingSignalEntity entity = repository.findByStrategyNameAndStockCodeAndSignalDateAndSignalType(
                        tradingSignal.strategyName(),
                        tradingSignal.stockCode(),
                        tradingSignal.signalDate(),
                        tradingSignal.signalType()
                )
                .orElseGet(() -> TradingSignalEntity.from(tradingSignal));
        entity.update(tradingSignal);
        repository.save(entity);
        return tradingSignal;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TradingSignal> find(
            String strategyName,
            String stockCode,
            LocalDate signalDate,
            SignalType signalType
    ) {
        return repository.findByStrategyNameAndStockCodeAndSignalDateAndSignalType(
                        strategyName,
                        stockCode,
                        signalDate,
                        signalType
                )
                .map(TradingSignalEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TradingSignal> findById(long signalId) {
        return repository.findById(signalId)
                .map(TradingSignalEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TradingSignalRecord> find(TradingSignalSearchCriteria criteria) {
        Specification<TradingSignalEntity> specification = (root, query, criteriaBuilder) ->
                criteriaBuilder.conjunction();
        if (criteria != null) {
            if (criteria.stockCode() != null) {
                specification = specification.and((root, query, criteriaBuilder) ->
                        criteriaBuilder.equal(root.get("stockCode"), criteria.stockCode()));
            }
            if (criteria.signalDate() != null) {
                specification = specification.and((root, query, criteriaBuilder) ->
                        criteriaBuilder.equal(root.get("signalDate"), criteria.signalDate()));
            }
            if (criteria.strategyName() != null) {
                specification = specification.and((root, query, criteriaBuilder) ->
                        criteriaBuilder.equal(root.get("strategyName"), criteria.strategyName()));
            }
            if (criteria.signalType() != null) {
                specification = specification.and((root, query, criteriaBuilder) ->
                        criteriaBuilder.equal(root.get("signalType"), criteria.signalType()));
            }
            if (criteria.status() != null) {
                specification = specification.and((root, query, criteriaBuilder) ->
                        criteriaBuilder.equal(root.get("status"), criteria.status()));
            }
            if (criteria.minScore() != null) {
                specification = specification.and((root, query, criteriaBuilder) ->
                        criteriaBuilder.greaterThanOrEqualTo(root.get("score"), criteria.minScore()));
            }
        }
        return repository.findAll(
                        specification,
                        Sort.by(Sort.Order.desc("signalDate"), Sort.Order.desc("id"))
                )
                .stream()
                .map(TradingSignalEntity::toRecord)
                .toList();
    }
}
