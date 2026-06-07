package seokhoon.trade.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import seokhoon.trade.domain.indicator.IndicatorSnapshot;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "indicator_snapshots",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_indicator_snapshot_stock_date",
                columnNames = {"stock_code", "trade_date"}
        )
)
public class IndicatorSnapshotEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "stock_code", nullable = false)
    private String stockCode;
    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;
    private BigDecimal ma5;
    private BigDecimal ma20;
    private BigDecimal ma60;
    private BigDecimal rsi14;
    private BigDecimal macd;
    private BigDecimal macdSignal;
    private BigDecimal macdHistogram;
    private BigDecimal bollingerUpper;
    private BigDecimal bollingerMiddle;
    private BigDecimal bollingerLower;

    protected IndicatorSnapshotEntity() {
    }

    public static IndicatorSnapshotEntity from(IndicatorSnapshot snapshot) {
        IndicatorSnapshotEntity entity = new IndicatorSnapshotEntity();
        entity.update(snapshot);
        return entity;
    }

    public void update(IndicatorSnapshot snapshot) {
        stockCode = snapshot.stockCode();
        tradeDate = snapshot.tradeDate();
        ma5 = snapshot.ma5();
        ma20 = snapshot.ma20();
        ma60 = snapshot.ma60();
        rsi14 = snapshot.rsi14();
        macd = snapshot.macd();
        macdSignal = snapshot.macdSignal();
        macdHistogram = snapshot.macdHistogram();
        bollingerUpper = snapshot.bollingerUpper();
        bollingerMiddle = snapshot.bollingerMiddle();
        bollingerLower = snapshot.bollingerLower();
    }

    public IndicatorSnapshot toDomain() {
        return new IndicatorSnapshot(
                stockCode,
                tradeDate,
                ma5,
                ma20,
                ma60,
                rsi14,
                macd,
                macdSignal,
                macdHistogram,
                bollingerUpper,
                bollingerMiddle,
                bollingerLower
        );
    }
}
