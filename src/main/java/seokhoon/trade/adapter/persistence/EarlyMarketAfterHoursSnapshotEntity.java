package seokhoon.trade.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import seokhoon.trade.domain.market.EarlyMarketAfterHoursSnapshot;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "early_market_after_hours_snapshots")
public class EarlyMarketAfterHoursSnapshotEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;
    @Column(name = "previous_trading_day", nullable = false)
    private LocalDate previousTradingDay;
    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;
    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;
    @Column(name = "after_hours_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal afterHoursPrice;
    @Column(name = "after_hours_change_rate", nullable = false, precision = 19, scale = 4)
    private BigDecimal afterHoursChangeRate;
    @Column(name = "after_hours_volume", nullable = false)
    private long afterHoursVolume;
    @Column(name = "after_hours_trading_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal afterHoursTradingValue;
    @Column(nullable = false, length = 100)
    private String source;

    protected EarlyMarketAfterHoursSnapshotEntity() {
    }

    static EarlyMarketAfterHoursSnapshotEntity from(
            EarlyMarketAfterHoursSnapshot snapshot
    ) {
        EarlyMarketAfterHoursSnapshotEntity entity =
                new EarlyMarketAfterHoursSnapshotEntity();
        entity.update(snapshot);
        return entity;
    }

    void update(EarlyMarketAfterHoursSnapshot snapshot) {
        tradeDate = snapshot.tradeDate();
        previousTradingDay = snapshot.previousTradingDay();
        capturedAt = snapshot.capturedAt();
        stockCode = snapshot.stockCode();
        afterHoursPrice = snapshot.afterHoursPrice();
        afterHoursChangeRate = snapshot.afterHoursChangeRate();
        afterHoursVolume = snapshot.afterHoursVolume();
        afterHoursTradingValue = snapshot.afterHoursTradingValue();
        source = snapshot.source();
    }

    EarlyMarketAfterHoursSnapshot toDomain() {
        return new EarlyMarketAfterHoursSnapshot(
                id,
                tradeDate,
                previousTradingDay,
                capturedAt,
                stockCode,
                afterHoursPrice,
                afterHoursChangeRate,
                afterHoursVolume,
                afterHoursTradingValue,
                source
        );
    }
}
