package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.research.DisclosureEvidenceImportHistory;

import java.util.List;

public interface DisclosureEvidenceImportHistoryPort {
    DisclosureEvidenceImportHistory save(DisclosureEvidenceImportHistory value);
    List<DisclosureEvidenceImportHistory> findRecentDisclosureImports(int limit);
    default List<DisclosureEvidenceImportHistory> findDisclosureImportsByStockCode(String stockCode, int limit) {
        return findRecentDisclosureImports(limit).stream()
                .filter(value -> java.util.Objects.equals(stockCode, value.stockCode())).toList();
    }
}
