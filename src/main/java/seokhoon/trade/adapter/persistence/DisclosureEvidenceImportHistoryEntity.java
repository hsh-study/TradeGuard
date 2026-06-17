package seokhoon.trade.adapter.persistence;

import jakarta.persistence.*;
import seokhoon.trade.domain.research.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "disclosure_evidence_import_histories")
public class DisclosureEvidenceImportHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private DisclosureProvider provider;
    @Column(name = "stock_code", length = 20)
    private String stockCode;
    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;
    @Column(name = "to_date", nullable = false)
    private LocalDate toDate;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DisclosureEvidenceImportStatus status;
    @Column(name = "imported_count", nullable = false)
    private int importedCount;
    @Column(name = "failure_reason", length = 1000)
    private String failureReason;
    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;
    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    protected DisclosureEvidenceImportHistoryEntity() {
    }

    static DisclosureEvidenceImportHistoryEntity from(DisclosureEvidenceImportHistory value) {
        DisclosureEvidenceImportHistoryEntity entity = new DisclosureEvidenceImportHistoryEntity();
        entity.id = value.id();
        entity.provider = value.provider();
        entity.stockCode = value.stockCode();
        entity.fromDate = value.fromDate();
        entity.toDate = value.toDate();
        entity.status = value.status();
        entity.importedCount = value.importedCount();
        entity.failureReason = value.failureReason();
        entity.requestedAt = value.requestedAt();
        entity.completedAt = value.completedAt();
        return entity;
    }

    DisclosureEvidenceImportHistory toDomain() {
        return new DisclosureEvidenceImportHistory(id, provider, stockCode, fromDate, toDate,
                status, importedCount, failureReason, requestedAt, completedAt);
    }
}
