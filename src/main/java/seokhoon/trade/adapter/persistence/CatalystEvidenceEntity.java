package seokhoon.trade.adapter.persistence;

import jakarta.persistence.*;
import seokhoon.trade.domain.research.*;

import java.time.Instant;

@Entity
@Table(name = "catalyst_evidences")
public class CatalystEvidenceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "catalyst_id")
    private Long catalystId;
    @Column(name = "stock_code", length = 20)
    private String stockCode;
    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_type", nullable = false, length = 40)
    private CatalystEvidenceType evidenceType;
    @Column(name = "title", nullable = false, length = 500)
    private String title;
    @Column(name = "summary", nullable = false, columnDefinition = "TEXT")
    private String summary;
    @Column(name = "source_name", length = 100)
    private String sourceName;
    @Column(name = "source_url", length = 1000)
    private String sourceUrl;
    @Column(name = "source_published_at")
    private Instant sourcePublishedAt;
    @Enumerated(EnumType.STRING)
    @Column(name = "confidence", nullable = false, length = 20)
    private EvidenceConfidence confidence;
    @Enumerated(EnumType.STRING)
    @Column(name = "created_by", nullable = false, length = 20)
    private EvidenceCreatedBy createdBy;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EvidenceStatus status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CatalystEvidenceEntity() {
    }

    static CatalystEvidenceEntity from(CatalystEvidence value) {
        CatalystEvidenceEntity entity = new CatalystEvidenceEntity();
        entity.id = value.id();
        entity.update(value);
        return entity;
    }

    void update(CatalystEvidence value) {
        catalystId = value.catalystId();
        stockCode = value.stockCode();
        evidenceType = value.evidenceType();
        title = value.title();
        summary = value.summary();
        sourceName = value.sourceName();
        sourceUrl = value.sourceUrl();
        sourcePublishedAt = value.sourcePublishedAt();
        confidence = value.confidence();
        createdBy = value.createdBy();
        status = value.status();
        createdAt = value.createdAt();
        updatedAt = value.updatedAt();
    }

    CatalystEvidence toDomain() {
        return new CatalystEvidence(id, catalystId, stockCode, evidenceType, title, summary,
                sourceName, sourceUrl, sourcePublishedAt, confidence, createdBy, status,
                createdAt, updatedAt);
    }
}
