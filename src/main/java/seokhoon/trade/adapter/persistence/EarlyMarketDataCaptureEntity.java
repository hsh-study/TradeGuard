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
import seokhoon.trade.domain.market.EarlyMarketCaptureStatus;
import seokhoon.trade.domain.market.EarlyMarketCaptureType;
import seokhoon.trade.domain.market.EarlyMarketDataCapture;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "early_market_data_captures",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_early_market_capture_date_type",
                columnNames = {"trade_date", "capture_type"}
        )
)
public class EarlyMarketDataCaptureEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;
    @Enumerated(EnumType.STRING)
    @Column(name = "capture_type", nullable = false, length = 50)
    private EarlyMarketCaptureType captureType;
    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;
    @Column(nullable = false, length = 100)
    private String source;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EarlyMarketCaptureStatus status;
    @Column(name = "item_count", nullable = false)
    private int itemCount;
    @Column(name = "failure_reason", length = 1000)
    private String failureReason;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected EarlyMarketDataCaptureEntity() {
    }

    static EarlyMarketDataCaptureEntity from(EarlyMarketDataCapture capture) {
        EarlyMarketDataCaptureEntity entity = new EarlyMarketDataCaptureEntity();
        entity.createdAt = capture.createdAt();
        entity.update(capture);
        return entity;
    }

    void update(EarlyMarketDataCapture capture) {
        tradeDate = capture.tradeDate();
        captureType = capture.captureType();
        capturedAt = capture.capturedAt();
        source = capture.source();
        status = capture.status();
        itemCount = capture.itemCount();
        failureReason = capture.failureReason();
    }

    EarlyMarketDataCapture toDomain() {
        return new EarlyMarketDataCapture(
                id,
                tradeDate,
                captureType,
                capturedAt,
                source,
                status,
                itemCount,
                failureReason,
                createdAt
        );
    }
}
