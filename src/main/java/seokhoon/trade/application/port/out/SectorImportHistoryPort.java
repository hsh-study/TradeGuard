package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.market.SectorImportHistory;

import java.util.List;

public interface SectorImportHistoryPort {
    SectorImportHistory save(SectorImportHistory history);
    List<SectorImportHistory> findRecentSectorImports(int limit);
}
