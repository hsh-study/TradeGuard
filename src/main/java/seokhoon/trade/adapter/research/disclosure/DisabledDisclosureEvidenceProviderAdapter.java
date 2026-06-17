package seokhoon.trade.adapter.research.disclosure;

import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.out.DisclosureEvidenceProviderPort;
import seokhoon.trade.domain.research.DisclosureEvidenceRecord;

import java.time.LocalDate;
import java.util.List;

@Component
public class DisabledDisclosureEvidenceProviderAdapter implements DisclosureEvidenceProviderPort {
    @Override
    public List<DisclosureEvidenceRecord> fetchDisclosures(String stockCode, LocalDate fromDate, LocalDate toDate) {
        return List.of();
    }
}
