package seokhoon.trade.adapter.persistence;

import jakarta.persistence.*;
import seokhoon.trade.domain.market.SectorDailySnapshot;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "sector_daily_snapshots", uniqueConstraints = @UniqueConstraint(
        name = "uk_sector_snapshot_code_date", columnNames = {"sector_code", "trade_date"}))
public class SectorDailySnapshotEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "sector_code", nullable = false, length = 50)
    private String sectorCode;
    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;
    @Column(name = "average_change_rate", nullable = false, precision = 10, scale = 4)
    private BigDecimal averageChangeRate;
    @Column(name = "median_change_rate", nullable = false, precision = 10, scale = 4)
    private BigDecimal medianChangeRate;
    @Column(name = "total_trading_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalTradingValue;
    @Column(name = "rising_stock_count", nullable = false)
    private int risingStockCount;
    @Column(name = "falling_stock_count", nullable = false)
    private int fallingStockCount;
    @Column(name = "leading_stock_code", length = 20)
    private String leadingStockCode;
    @Column(name = "leading_stock_change_rate", precision = 10, scale = 4)
    private BigDecimal leadingStockChangeRate;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SectorDailySnapshotEntity() {
    }

    static SectorDailySnapshotEntity from(SectorDailySnapshot value) {
        SectorDailySnapshotEntity entity = new SectorDailySnapshotEntity();
        entity.id = value.id();
        entity.update(value);
        return entity;
    }

    void update(SectorDailySnapshot value) {
        sectorCode = value.sectorCode();
        tradeDate = value.tradeDate();
        averageChangeRate = value.averageChangeRate();
        medianChangeRate = value.medianChangeRate();
        totalTradingValue = value.totalTradingValue();
        risingStockCount = value.risingStockCount();
        fallingStockCount = value.fallingStockCount();
        leadingStockCode = value.leadingStockCode();
        leadingStockChangeRate = value.leadingStockChangeRate();
        createdAt = value.createdAt();
        updatedAt = value.updatedAt();
    }

    SectorDailySnapshot toDomain() {
        return new SectorDailySnapshot(id, sectorCode, tradeDate, averageChangeRate,
                medianChangeRate, totalTradingValue, risingStockCount, fallingStockCount,
                leadingStockCode, leadingStockChangeRate, createdAt, updatedAt);
    }
}
