package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.research.DisclosureEvidenceImportHistory;

import java.util.List;

public interface DisclosureEvidenceImportHistoryPort {
    DisclosureEvidenceImportHistory save(DisclosureEvidenceImportHistory value);
    List<DisclosureEvidenceImportHistory> findRecentDisclosureImports(int limit);
}
