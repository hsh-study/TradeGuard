package seokhoon.trade.adapter.persistence;

import jakarta.persistence.*;
import seokhoon.trade.domain.research.SharesOutstandingImportHistory;
import seokhoon.trade.domain.research.SharesOutstandingImportStatus;

import java.time.Instant;

@Entity
@Table(name = "shares_outstanding_import_histories")
public class SharesOutstandingImportHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SharesOutstandingImportStatus status;
    @Column(name = "imported_count", nullable = false)
    private int importedCount;
    @Column(name = "failure_reason", length = 1000)
    private String failureReason;
    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;
    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    protected SharesOutstandingImportHistoryEntity() {
    }

    static SharesOutstandingImportHistoryEntity from(SharesOutstandingImportHistory value) {
        SharesOutstandingImportHistoryEntity entity = new SharesOutstandingImportHistoryEntity();
        entity.id = value.id();
        entity.status = value.status();
        entity.importedCount = value.importedCount();
        entity.failureReason = value.failureReason();
        entity.requestedAt = value.requestedAt();
        entity.completedAt = value.completedAt();
        return entity;
    }

    SharesOutstandingImportHistory toDomain() {
        return new SharesOutstandingImportHistory(id, status, importedCount,
                failureReason, requestedAt, completedAt);
    }
}
