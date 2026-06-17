package seokhoon.trade.adapter.persistence;

import jakarta.persistence.*;
import seokhoon.trade.domain.research.DartCorpCodeImportHistory;
import seokhoon.trade.domain.research.DartCorpCodeImportStatus;

import java.time.Instant;

@Entity
@Table(name = "dart_corp_code_import_histories")
public class DartCorpCodeImportHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DartCorpCodeImportStatus status;
    @Column(name = "imported_count", nullable = false)
    private int importedCount;
    @Column(name = "matched_stock_count", nullable = false)
    private int matchedStockCount;
    @Column(name = "failure_reason", length = 1000)
    private String failureReason;
    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;
    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    protected DartCorpCodeImportHistoryEntity() {
    }

    static DartCorpCodeImportHistoryEntity from(DartCorpCodeImportHistory value) {
        DartCorpCodeImportHistoryEntity entity = new DartCorpCodeImportHistoryEntity();
        entity.id = value.id();
        entity.status = value.status();
        entity.importedCount = value.importedCount();
        entity.matchedStockCount = value.matchedStockCount();
        entity.failureReason = value.failureReason();
        entity.requestedAt = value.requestedAt();
        entity.completedAt = value.completedAt();
        return entity;
    }

    DartCorpCodeImportHistory toDomain() {
        return new DartCorpCodeImportHistory(id, status, importedCount, matchedStockCount,
                failureReason, requestedAt, completedAt);
    }
}
