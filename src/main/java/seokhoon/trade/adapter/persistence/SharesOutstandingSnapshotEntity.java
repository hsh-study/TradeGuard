package seokhoon.trade.adapter.persistence;

import jakarta.persistence.*;
import seokhoon.trade.domain.research.SharesOutstandingSnapshot;
import seokhoon.trade.domain.research.SharesOutstandingSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "shares_outstanding_snapshots", uniqueConstraints = @UniqueConstraint(
        name = "uk_shares_outstanding_stock_date", columnNames = {"stock_code", "base_date"}))
public class SharesOutstandingSnapshotEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;
    @Column(name = "base_date", nullable = false)
    private LocalDate baseDate;
    @Column(name = "shares_outstanding", nullable = false, precision = 19, scale = 4)
    private BigDecimal sharesOutstanding;
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private SharesOutstandingSource source;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SharesOutstandingSnapshotEntity() {
    }

    static SharesOutstandingSnapshotEntity from(SharesOutstandingSnapshot value) {
        SharesOutstandingSnapshotEntity entity = new SharesOutstandingSnapshotEntity();
        entity.id = value.id();
        entity.update(value);
        return entity;
    }

    void update(SharesOutstandingSnapshot value) {
        stockCode = value.stockCode();
        baseDate = value.baseDate();
        sharesOutstanding = value.sharesOutstanding();
        source = value.source();
        createdAt = value.createdAt();
        updatedAt = value.updatedAt();
    }

    SharesOutstandingSnapshot toDomain() {
        return new SharesOutstandingSnapshot(id, stockCode, baseDate, sharesOutstanding,
                source, createdAt, updatedAt);
    }
}
