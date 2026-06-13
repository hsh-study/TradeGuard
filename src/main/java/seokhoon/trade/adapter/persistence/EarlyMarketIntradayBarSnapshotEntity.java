package seokhoon.trade.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import seokhoon.trade.domain.market.BarInterval;
import seokhoon.trade.domain.market.EarlyMarketIntradayBarSnapshot;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "early_market_intraday_bar_snapshots")
public class EarlyMarketIntradayBarSnapshotEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;
    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;
    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;
    @Column(name = "bar_time", nullable = false)
    private LocalTime barTime;
    @Enumerated(EnumType.STRING)
    @Column(name = "interval_type", nullable = false, length = 30)
    private BarInterval intervalType;
    @Column(name = "open_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal openPrice;
    @Column(name = "high_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal highPrice;
    @Column(name = "low_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal lowPrice;
    @Column(name = "close_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal closePrice;
    @Column(nullable = false)
    private long volume;
    @Column(name = "trading_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal tradingValue;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal vwap;
    @Column(nullable = false, length = 100)
    private String source;

    protected EarlyMarketIntradayBarSnapshotEntity() {
    }

    static EarlyMarketIntradayBarSnapshotEntity from(
            EarlyMarketIntradayBarSnapshot snapshot
    ) {
        EarlyMarketIntradayBarSnapshotEntity entity =
                new EarlyMarketIntradayBarSnapshotEntity();
        entity.update(snapshot);
        return entity;
    }

    void update(EarlyMarketIntradayBarSnapshot snapshot) {
        tradeDate = snapshot.tradeDate();
        stockCode = snapshot.stockCode();
        capturedAt = snapshot.capturedAt();
        barTime = snapshot.barTime();
        intervalType = snapshot.intervalType();
        openPrice = snapshot.openPrice();
        highPrice = snapshot.highPrice();
        lowPrice = snapshot.lowPrice();
        closePrice = snapshot.closePrice();
        volume = snapshot.volume();
        tradingValue = snapshot.tradingValue();
        vwap = snapshot.vwap();
        source = snapshot.source();
    }

    EarlyMarketIntradayBarSnapshot toDomain() {
        return new EarlyMarketIntradayBarSnapshot(
                id,
                tradeDate,
                stockCode,
                capturedAt,
                barTime,
                intervalType,
                openPrice,
                highPrice,
                lowPrice,
                closePrice,
                volume,
                tradingValue,
                vwap,
                source
        );
    }
}
