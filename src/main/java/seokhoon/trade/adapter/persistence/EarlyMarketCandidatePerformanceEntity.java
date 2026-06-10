package seokhoon.trade.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import seokhoon.trade.domain.market.EarlyMarketCandidatePerformance;
import seokhoon.trade.domain.strategy.SignalType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "early_market_candidate_performances",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_early_market_performance_signal",
                columnNames = "signal_id"
        )
)
public class EarlyMarketCandidatePerformanceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "signal_id", nullable = false)
    private Long signalId;
    @Column(name = "stock_code", nullable = false)
    private String stockCode;
    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;
    @Enumerated(EnumType.STRING)
    @Column(name = "signal_type", nullable = false)
    private SignalType signalType;
    @Column(name = "entry_reference_price", precision = 19, scale = 4)
    private BigDecimal entryReferencePrice;
    @Column(name = "high_until_0930", precision = 19, scale = 4)
    private BigDecimal highUntil0930;
    @Column(name = "low_until_0930", precision = 19, scale = 4)
    private BigDecimal lowUntil0930;
    @Column(name = "price_at_0930", precision = 19, scale = 4)
    private BigDecimal priceAt0930;
    @Column(name = "max_return_rate_until_0930", precision = 19, scale = 4)
    private BigDecimal maxReturnRateUntil0930;
    @Column(name = "max_drawdown_rate_until_0930", precision = 19, scale = 4)
    private BigDecimal maxDrawdownRateUntil0930;
    @Column(name = "vwap_broken")
    private Boolean vwapBroken;
    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    protected EarlyMarketCandidatePerformanceEntity() {
    }

    static EarlyMarketCandidatePerformanceEntity from(
            EarlyMarketCandidatePerformance performance
    ) {
        EarlyMarketCandidatePerformanceEntity entity =
                new EarlyMarketCandidatePerformanceEntity();
        entity.update(performance);
        return entity;
    }

    void update(EarlyMarketCandidatePerformance performance) {
        signalId = performance.signalId();
        stockCode = performance.stockCode();
        tradeDate = performance.tradeDate();
        signalType = performance.signalType();
        entryReferencePrice = performance.entryReferencePrice();
        highUntil0930 = performance.highUntil0930();
        lowUntil0930 = performance.lowUntil0930();
        priceAt0930 = performance.priceAt0930();
        maxReturnRateUntil0930 = performance.maxReturnRateUntil0930();
        maxDrawdownRateUntil0930 = performance.maxDrawdownRateUntil0930();
        vwapBroken = performance.vwapBroken();
        capturedAt = performance.capturedAt();
    }

    EarlyMarketCandidatePerformance toDomain() {
        return new EarlyMarketCandidatePerformance(
                signalId,
                stockCode,
                tradeDate,
                signalType,
                entryReferencePrice,
                highUntil0930,
                lowUntil0930,
                priceAt0930,
                maxReturnRateUntil0930,
                maxDrawdownRateUntil0930,
                vwapBroken,
                capturedAt
        );
    }
}
