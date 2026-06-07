package seokhoon.trade.adapter.persistence;

import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.out.IndicatorSnapshotPort;
import seokhoon.trade.domain.indicator.IndicatorSnapshot;

import java.time.LocalDate;
import java.util.List;

@Component
public class IndicatorSnapshotPersistenceAdapter implements IndicatorSnapshotPort {
    private final IndicatorSnapshotJpaRepository repository;

    public IndicatorSnapshotPersistenceAdapter(IndicatorSnapshotJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public IndicatorSnapshot save(IndicatorSnapshot snapshot) {
        IndicatorSnapshotEntity entity = repository
                .findByStockCodeAndTradeDate(snapshot.stockCode(), snapshot.tradeDate())
                .orElseGet(() -> IndicatorSnapshotEntity.from(snapshot));
        entity.update(snapshot);
        return repository.save(entity).toDomain();
    }

    @Override
    public List<IndicatorSnapshot> findByStockCodeAndTradeDateBetween(
            String stockCode,
            LocalDate from,
            LocalDate to
    ) {
        return repository.findByStockCodeAndTradeDateBetweenOrderByTradeDateAsc(stockCode, from, to).stream()
                .map(IndicatorSnapshotEntity::toDomain)
                .toList();
    }
}
