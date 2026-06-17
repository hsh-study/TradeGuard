package seokhoon.trade.adapter.persistence;

import jakarta.persistence.*;
import seokhoon.trade.domain.market.MarketIndexImportHistory;
import seokhoon.trade.domain.market.MarketIndexImportProvider;
import seokhoon.trade.domain.market.MarketIndexImportStatus;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "market_index_import_histories")
public class MarketIndexImportHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 30)
    private MarketIndexImportProvider provider;
    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private MarketIndexImportStatus status;
    @Column(name = "imported_count", nullable = false)
    private int importedCount;
    @Column(name = "failure_reason", length = 1000)
    private String failureReason;
    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;
    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    protected MarketIndexImportHistoryEntity() {
    }

    static MarketIndexImportHistoryEntity from(MarketIndexImportHistory value) {
        MarketIndexImportHistoryEntity entity = new MarketIndexImportHistoryEntity();
        entity.id = value.id();
        entity.provider = value.provider();
        entity.tradeDate = value.tradeDate();
        entity.status = value.status();
        entity.importedCount = value.importedCount();
        entity.failureReason = value.failureReason();
        entity.requestedAt = value.requestedAt();
        entity.completedAt = value.completedAt();
        return entity;
    }

    MarketIndexImportHistory toDomain() {
        return new MarketIndexImportHistory(id, provider, tradeDate, status,
                importedCount, failureReason, requestedAt, completedAt);
    }
}
