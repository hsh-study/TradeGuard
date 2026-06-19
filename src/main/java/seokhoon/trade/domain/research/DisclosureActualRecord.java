package seokhoon.trade.domain.research;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

public record DisclosureActualRecord(
        String stockCode,
        LocalDate disclosureDate,
        LocalTime disclosureTime,
        String title,
        String disclosureType,
        DisclosureProvider source,
        String sourceUrl,
        String receiptNo,
        CatalystType relatedCatalystType,
        CatalystImportance importance,
        String rawCategory
) {
    public DisclosureActualRecord {
        Objects.requireNonNull(stockCode, "stockCode");
        Objects.requireNonNull(disclosureDate, "disclosureDate");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(disclosureType, "disclosureType");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(sourceUrl, "sourceUrl");
        Objects.requireNonNull(importance, "importance");
    }
}
