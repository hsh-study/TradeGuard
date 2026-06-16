package seokhoon.trade.adapter.persistence;

import jakarta.persistence.*;
import seokhoon.trade.domain.research.ValuationSnapshot;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "valuation_snapshots", uniqueConstraints = @UniqueConstraint(
        name = "uk_valuation_snapshot_stock_date", columnNames = {"stock_code", "trade_date"}))
public class ValuationSnapshotEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;
    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;
    @Column(name = "market_cap", nullable = false, precision = 19, scale = 4)
    private BigDecimal marketCap;
    @Column(name = "per", precision = 19, scale = 4)
    private BigDecimal per;
    @Column(name = "pbr", precision = 19, scale = 4)
    private BigDecimal pbr;
    @Column(name = "psr", precision = 19, scale = 4)
    private BigDecimal psr;
    @Column(name = "eps", precision = 19, scale = 4)
    private BigDecimal eps;
    @Column(name = "bps", precision = 19, scale = 4)
    private BigDecimal bps;
    @Column(name = "sales_per_share", precision = 19, scale = 4)
    private BigDecimal salesPerShare;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ValuationSnapshotEntity() {
    }

    static ValuationSnapshotEntity from(ValuationSnapshot value) {
        ValuationSnapshotEntity entity = new ValuationSnapshotEntity();
        entity.id = value.id();
        entity.update(value);
        return entity;
    }

    void update(ValuationSnapshot value) {
        stockCode = value.stockCode();
        tradeDate = value.tradeDate();
        marketCap = value.marketCap();
        per = value.per();
        pbr = value.pbr();
        psr = value.psr();
        eps = value.eps();
        bps = value.bps();
        salesPerShare = value.salesPerShare();
        createdAt = value.createdAt();
        updatedAt = value.updatedAt();
    }

    ValuationSnapshot toDomain() {
        return new ValuationSnapshot(id, stockCode, tradeDate, marketCap, per, pbr, psr,
                eps, bps, salesPerShare, createdAt, updatedAt);
    }
}
