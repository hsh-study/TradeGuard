package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.research.DisclosureActualRecord;

import java.time.LocalDate;
import java.util.List;

public interface DisclosureActualProviderPort {
    List<DisclosureActualRecord> fetchDisclosures(String stockCode, LocalDate fromDate, LocalDate toDate);
}
