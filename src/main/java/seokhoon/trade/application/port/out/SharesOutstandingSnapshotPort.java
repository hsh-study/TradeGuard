package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.research.SharesOutstandingSnapshot;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SharesOutstandingSnapshotPort {
    SharesOutstandingSnapshot save(SharesOutstandingSnapshot value);
    Optional<SharesOutstandingSnapshot> findLatestSharesByStockCode(String stockCode, LocalDate baseDate);
    List<SharesOutstandingSnapshot> findSharesByStockCode(String stockCode);
}
