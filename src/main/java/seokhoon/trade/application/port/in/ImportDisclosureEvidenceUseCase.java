package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.research.DisclosureEvidenceImportHistory;

import java.time.LocalDate;
import java.util.List;

public interface ImportDisclosureEvidenceUseCase {
    DisclosureEvidenceImportHistory importDisclosures(String stockCode, LocalDate from, LocalDate to);
    List<DisclosureEvidenceImportHistory> findDisclosureImportHistories();
}
