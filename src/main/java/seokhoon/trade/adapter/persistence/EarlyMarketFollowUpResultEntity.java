package seokhoon.trade.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import seokhoon.trade.application.port.in.EarlyMarketFollowUpDecision;
import seokhoon.trade.domain.market.EarlyMarketFollowUpRecord;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(
        name = "early_market_follow_up_results",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_early_market_follow_up_signal",
                columnNames = "signal_id"
        )
)
public class EarlyMarketFollowUpResultEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "signal_id", nullable = false)
    private Long signalId;
    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;
    @Column(name = "stock_code", nullable = false)
    private String stockCode;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EarlyMarketFollowUpDecision decision;
    @Column(name = "signal_score", nullable = false)
    private int signalScore;
    @Column(name = "last_price", precision = 19, scale = 4)
    private BigDecimal lastPrice;
    @Column(name = "high_since_0905", precision = 19, scale = 4)
    private BigDecimal highSince0905;
    @Column(name = "drawdown_from_high", precision = 19, scale = 4)
    private BigDecimal drawdownFromHigh;
    @Column(name = "vwap_broken")
    private Boolean vwapBroken;
    @Convert(converter = StringListJsonConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private List<String> reasons = List.of();
    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    protected EarlyMarketFollowUpResultEntity() {
    }

    static EarlyMarketFollowUpResultEntity from(EarlyMarketFollowUpRecord result) {
        EarlyMarketFollowUpResultEntity entity = new EarlyMarketFollowUpResultEntity();
        entity.update(result);
        return entity;
    }

    void update(EarlyMarketFollowUpRecord result) {
        signalId = result.signalId();
        tradeDate = result.tradeDate();
        stockCode = result.stockCode();
        decision = result.decision();
        signalScore = result.signalScore();
        lastPrice = result.lastPrice();
        highSince0905 = result.highSince0905();
        drawdownFromHigh = result.drawdownFromHigh();
        vwapBroken = result.vwapBroken();
        reasons = List.copyOf(result.reasons());
        capturedAt = result.capturedAt();
    }

    EarlyMarketFollowUpRecord toDomain() {
        return new EarlyMarketFollowUpRecord(
                signalId,
                tradeDate,
                stockCode,
                decision,
                signalScore,
                lastPrice,
                highSince0905,
                drawdownFromHigh,
                vwapBroken,
                List.copyOf(reasons),
                capturedAt
        );
    }
}
