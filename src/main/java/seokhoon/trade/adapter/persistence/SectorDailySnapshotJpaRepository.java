package seokhoon.trade.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SectorDailySnapshotJpaRepository extends JpaRepository<SectorDailySnapshotEntity, Long> {
    Optional<SectorDailySnapshotEntity> findBySectorCodeAndTradeDate(String sectorCode, LocalDate tradeDate);
    List<SectorDailySnapshotEntity> findByTradeDateOrderByAverageChangeRateDesc(LocalDate tradeDate);
}
