package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.research.ValuationSnapshot;

import java.time.LocalDate;
import java.util.Optional;

public interface ValuationSnapshotPort {
    ValuationSnapshot save(ValuationSnapshot value);
    Optional<ValuationSnapshot> findLatestByStockCode(String stockCode, LocalDate baseDate);
}
