package seokhoon.trade.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import seokhoon.trade.domain.market.EarlyMarketMarketSnapshot;
import seokhoon.trade.domain.market.EarlyMarketSnapshotType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "early_market_market_snapshots")
public class EarlyMarketMarketSnapshotEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;
    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;
    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;
    @Enumerated(EnumType.STRING)
    @Column(name = "snapshot_type", nullable = false, length = 30)
    private EarlyMarketSnapshotType snapshotType;
    @Column(name = "current_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal currentPrice;
    @Column(name = "day_high", nullable = false, precision = 19, scale = 4)
    private BigDecimal dayHigh;
    @Column(name = "day_low", nullable = false, precision = 19, scale = 4)
    private BigDecimal dayLow;
    @Column(name = "accumulated_volume", nullable = false)
    private long accumulatedVolume;
    @Column(name = "accumulated_trading_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal accumulatedTradingValue;
    @Column(precision = 19, scale = 4)
    private BigDecimal vwap;
    @Column(nullable = false, length = 100)
    private String source;

    protected EarlyMarketMarketSnapshotEntity() {
    }

    static EarlyMarketMarketSnapshotEntity from(
            EarlyMarketMarketSnapshot snapshot
    ) {
        EarlyMarketMarketSnapshotEntity entity =
                new EarlyMarketMarketSnapshotEntity();
        entity.update(snapshot);
        return entity;
    }

    void update(EarlyMarketMarketSnapshot snapshot) {
        tradeDate = snapshot.tradeDate();
        stockCode = snapshot.stockCode();
        capturedAt = snapshot.capturedAt();
        snapshotType = snapshot.snapshotType();
        currentPrice = snapshot.currentPrice();
        dayHigh = snapshot.dayHigh();
        dayLow = snapshot.dayLow();
        accumulatedVolume = snapshot.accumulatedVolume();
        accumulatedTradingValue = snapshot.accumulatedTradingValue();
        vwap = snapshot.vwap();
        source = snapshot.source();
    }

    EarlyMarketMarketSnapshot toDomain() {
        return new EarlyMarketMarketSnapshot(
                id,
                tradeDate,
                stockCode,
                capturedAt,
                snapshotType,
                currentPrice,
                dayHigh,
                dayLow,
                accumulatedVolume,
                accumulatedTradingValue,
                vwap,
                source
        );
    }
}
