package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.research.CatalystEvidence;
import seokhoon.trade.domain.research.DisclosureEvidenceImportHistory;

import java.time.LocalDate;
import java.util.List;

public interface ImportDisclosureActualEvidenceUseCase {
    DisclosureEvidenceImportHistory importStock(String stockCode, LocalDate fromDate, LocalDate toDate);
    List<DisclosureEvidenceImportHistory> importWatchlist(LocalDate baseDate);
    List<DisclosureEvidenceImportHistory> importHoldings(LocalDate baseDate);
    List<DisclosureEvidenceImportHistory> findHistories(String stockCode);
    List<CatalystEvidence> findEvidences(String stockCode, LocalDate fromDate, LocalDate toDate);
}
