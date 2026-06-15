package seokhoon.trade.adapter.persistence;

import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.out.IndicatorWarmUpHistoryPort;
import seokhoon.trade.domain.indicator.*;

import java.time.Instant;
import java.util.List;

@Component
public class IndicatorWarmUpHistoryPersistenceAdapter
        implements IndicatorWarmUpHistoryPort {
    private final IndicatorWarmUpHistoryJpaRepository repository;

    public IndicatorWarmUpHistoryPersistenceAdapter(
            IndicatorWarmUpHistoryJpaRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    public IndicatorWarmUpHistory save(
            IndicatorWarmUpResult result,
            String failureReason,
            Instant createdAt
    ) {
        return repository.save(
                IndicatorWarmUpHistoryEntity.from(
                        result,
                        failureReason,
                        createdAt
                )
        ).toDomain();
    }

    @Override
    public List<IndicatorWarmUpHistory> findByStockCode(
            String stockCode
    ) {
        return repository
                .findByStockCodeOrderByCreatedAtDescIdDesc(stockCode)
                .stream()
                .map(IndicatorWarmUpHistoryEntity::toDomain)
                .toList();
    }
}
