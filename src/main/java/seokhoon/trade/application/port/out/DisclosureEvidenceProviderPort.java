package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.research.DisclosureEvidenceRecord;

import java.time.LocalDate;
import java.util.List;

public interface DisclosureEvidenceProviderPort {
    List<DisclosureEvidenceRecord> fetchDisclosures(String stockCode, LocalDate fromDate, LocalDate toDate);
}
