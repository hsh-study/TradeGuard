package seokhoon.trade.adapter.persistence;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.out.SectorDailySnapshotPort;
import seokhoon.trade.domain.market.SectorDailySnapshot;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class SectorDailySnapshotPersistenceAdapter implements SectorDailySnapshotPort {
    private final SectorDailySnapshotJpaRepository repository;

    public SectorDailySnapshotPersistenceAdapter(SectorDailySnapshotJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public SectorDailySnapshot save(SectorDailySnapshot snapshot) {
        SectorDailySnapshotEntity entity = repository
                .findBySectorCodeAndTradeDate(snapshot.sectorCode(), snapshot.tradeDate())
                .orElseGet(() -> SectorDailySnapshotEntity.from(snapshot));
        entity.update(snapshot);
        return repository.save(entity).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SectorDailySnapshot> findBySectorCodeAndTradeDate(String sectorCode, LocalDate tradeDate) {
        return repository.findBySectorCodeAndTradeDate(sectorCode, tradeDate)
                .map(SectorDailySnapshotEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SectorDailySnapshot> findByTradeDate(LocalDate tradeDate) {
        return repository.findByTradeDateOrderByAverageChangeRateDesc(tradeDate)
                .stream().map(SectorDailySnapshotEntity::toDomain).toList();
    }
}
