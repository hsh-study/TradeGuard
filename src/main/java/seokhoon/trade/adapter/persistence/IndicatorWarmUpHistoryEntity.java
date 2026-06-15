package seokhoon.trade.adapter.persistence;

import jakarta.persistence.*;
import seokhoon.trade.domain.indicator.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "indicator_warmup_histories")
class IndicatorWarmUpHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "stock_code", nullable = false)
    private String stockCode;
    @Column(name = "base_date", nullable = false)
    private LocalDate baseDate;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IndicatorWarmUpStatus status;
    @Column(name = "imported_daily_price_count", nullable = false)
    private int importedDailyPriceCount;
    @Column(name = "total_daily_price_count", nullable = false)
    private int totalDailyPriceCount;
    @Column(name = "sufficient_for_ma20", nullable = false)
    private boolean sufficientForMa20;
    @Column(name = "sufficient_for_ma60", nullable = false)
    private boolean sufficientForMa60;
    @Column(name = "failure_reason", length = 1000)
    private String failureReason;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected IndicatorWarmUpHistoryEntity() {
    }

    static IndicatorWarmUpHistoryEntity from(
            IndicatorWarmUpResult result,
            String failureReason,
            Instant createdAt
    ) {
        IndicatorWarmUpHistoryEntity entity =
                new IndicatorWarmUpHistoryEntity();
        entity.stockCode = result.stockCode();
        entity.baseDate = result.baseDate();
        entity.status = result.status();
        entity.importedDailyPriceCount =
                result.importedDailyPriceCount();
        entity.totalDailyPriceCount = result.totalDailyPriceCount();
        entity.sufficientForMa20 = result.sufficientForMa20();
        entity.sufficientForMa60 = result.sufficientForMa60();
        entity.failureReason = failureReason;
        entity.createdAt = createdAt;
        return entity;
    }

    IndicatorWarmUpHistory toDomain() {
        return new IndicatorWarmUpHistory(
                id,
                stockCode,
                baseDate,
                status,
                importedDailyPriceCount,
                totalDailyPriceCount,
                sufficientForMa20,
                sufficientForMa60,
                failureReason,
                createdAt
        );
    }
}
