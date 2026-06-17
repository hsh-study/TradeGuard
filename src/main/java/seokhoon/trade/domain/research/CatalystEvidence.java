package seokhoon.trade.domain.research;

import java.time.Instant;
import java.util.Objects;

public record CatalystEvidence(
        Long id,
        Long catalystId,
        String stockCode,
        CatalystEvidenceType evidenceType,
        String title,
        String summary,
        String sourceName,
        String sourceUrl,
        Instant sourcePublishedAt,
        EvidenceConfidence confidence,
        EvidenceCreatedBy createdBy,
        EvidenceStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public CatalystEvidence {
        Objects.requireNonNull(evidenceType, "evidenceType");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(summary, "summary");
        Objects.requireNonNull(confidence, "confidence");
        Objects.requireNonNull(createdBy, "createdBy");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }
}
