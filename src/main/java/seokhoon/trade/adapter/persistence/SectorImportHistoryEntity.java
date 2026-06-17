package seokhoon.trade.adapter.persistence;

import jakarta.persistence.*;
import seokhoon.trade.domain.market.SectorImportHistory;
import seokhoon.trade.domain.market.SectorImportStatus;

import java.time.Instant;

@Entity
@Table(name = "sector_import_histories")
public class SectorImportHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SectorImportStatus status;
    @Column(name = "imported_sector_count", nullable = false)
    private int importedSectorCount;
    @Column(name = "imported_mapping_count", nullable = false)
    private int importedMappingCount;
    @Column(name = "failure_reason", length = 1000)
    private String failureReason;
    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;
    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    protected SectorImportHistoryEntity() {
    }

    static SectorImportHistoryEntity from(SectorImportHistory value) {
        SectorImportHistoryEntity entity = new SectorImportHistoryEntity();
        entity.id = value.id();
        entity.status = value.status();
        entity.importedSectorCount = value.importedSectorCount();
        entity.importedMappingCount = value.importedMappingCount();
        entity.failureReason = value.failureReason();
        entity.requestedAt = value.requestedAt();
        entity.completedAt = value.completedAt();
        return entity;
    }

    SectorImportHistory toDomain() {
        return new SectorImportHistory(id, status, importedSectorCount,
                importedMappingCount, failureReason, requestedAt, completedAt);
    }
}
