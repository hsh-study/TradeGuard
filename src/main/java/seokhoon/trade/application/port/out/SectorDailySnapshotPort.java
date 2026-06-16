package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.market.SectorDailySnapshot;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SectorDailySnapshotPort {
    SectorDailySnapshot save(SectorDailySnapshot snapshot);
    Optional<SectorDailySnapshot> findBySectorCodeAndTradeDate(String sectorCode, LocalDate tradeDate);
    List<SectorDailySnapshot> findByTradeDate(LocalDate tradeDate);
}
