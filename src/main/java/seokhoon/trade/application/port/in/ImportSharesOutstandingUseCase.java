package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.research.SharesOutstandingImportHistory;

import java.util.List;

public interface ImportSharesOutstandingUseCase {
    SharesOutstandingImportHistory importCsv(String csv);
    List<SharesOutstandingImportHistory> findSharesOutstandingImportHistories();
}
