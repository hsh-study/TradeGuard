package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.research.SharesOutstandingImportHistory;

import java.util.List;

public interface SharesOutstandingImportHistoryPort {
    SharesOutstandingImportHistory save(SharesOutstandingImportHistory value);
    List<SharesOutstandingImportHistory> findAllSharesOutstandingImports();
}
