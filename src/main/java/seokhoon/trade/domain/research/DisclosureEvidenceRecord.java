package seokhoon.trade.domain.research;

import java.time.Instant;

public record DisclosureEvidenceRecord(
        String stockCode,
        CatalystEvidenceType evidenceType,
        String title,
        String summary,
        String sourceName,
        String sourceUrl,
        Instant sourcePublishedAt,
        EvidenceConfidence confidence
) {
}
