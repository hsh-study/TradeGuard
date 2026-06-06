package seokhoon.trade.adapter.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "indicator_snapshots")
public class IndicatorSnapshotEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String stockCode;
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
}
