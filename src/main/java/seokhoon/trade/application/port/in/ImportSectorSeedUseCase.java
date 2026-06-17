package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.market.SectorImportHistory;

import java.util.List;

public interface ImportSectorSeedUseCase {
    SectorImportHistory importCsv(String csv);
    List<SectorImportHistory> findSectorImportHistories();
}
