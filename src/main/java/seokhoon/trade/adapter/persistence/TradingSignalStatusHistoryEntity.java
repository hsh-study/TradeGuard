package seokhoon.trade.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import seokhoon.trade.application.port.out.SignalStatusHistoryRecord;
import seokhoon.trade.domain.strategy.TradingSignalStatus;

import java.time.Instant;

@Entity
@Table(name = "trading_signal_status_histories")
public class TradingSignalStatusHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "trading_signal_id", nullable = false)
    private long tradingSignalId;
    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", nullable = false)
    private TradingSignalStatus fromStatus;
    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    private TradingSignalStatus toStatus;
    @Column(length = 1000)
    private String reason;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TradingSignalStatusHistoryEntity() {
    }

    TradingSignalStatusHistoryEntity(
            long tradingSignalId,
            TradingSignalStatus fromStatus,
            TradingSignalStatus toStatus,
            String reason,
            Instant createdAt
    ) {
        this.tradingSignalId = tradingSignalId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    SignalStatusHistoryRecord toRecord() {
        return new SignalStatusHistoryRecord(
                id,
                tradingSignalId,
                fromStatus,
                toStatus,
                reason,
                createdAt
        );
    }
}
