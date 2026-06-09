package seokhoon.trade.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import seokhoon.trade.domain.strategy.SignalType;
import seokhoon.trade.domain.strategy.TradingSignal;
import seokhoon.trade.domain.strategy.TradingSignalStatus;
import seokhoon.trade.application.port.out.TradingSignalRecord;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "trading_signals",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_trading_signal_strategy_stock_date_type",
                columnNames = {"strategy_name", "stock_code", "signal_date", "signal_type"}
        )
)
public class TradingSignalEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "strategy_name", nullable = false)
    private String strategyName;
    @Column(name = "stock_code", nullable = false)
    private String stockCode;
    @Column(name = "signal_date", nullable = false)
    private LocalDate signalDate;
    @Enumerated(EnumType.STRING)
    @Column(name = "signal_type", nullable = false)
    private SignalType signalType;
    private int score;
    @ElementCollection
    @CollectionTable(name = "trading_signal_reasons", joinColumns = @JoinColumn(name = "trading_signal_id"))
    private List<String> reasons = new ArrayList<>();
    @ElementCollection
    @CollectionTable(name = "trading_signal_risk_reasons", joinColumns = @JoinColumn(name = "trading_signal_id"))
    private List<String> riskReasons = new ArrayList<>();
    @Enumerated(EnumType.STRING)
    private TradingSignalStatus status;

    protected TradingSignalEntity() {
    }

    public static TradingSignalEntity from(TradingSignal signal) {
        TradingSignalEntity entity = new TradingSignalEntity();
        entity.update(signal);
        return entity;
    }

    public void update(TradingSignal signal) {
        strategyName = signal.strategyName();
        stockCode = signal.stockCode();
        signalDate = signal.signalDate();
        signalType = signal.signalType();
        score = signal.score();
        reasons.clear();
        reasons.addAll(signal.reasons());
        riskReasons.clear();
        riskReasons.addAll(signal.riskReasons());
        status = signal.status();
    }

    TradingSignalStatus status() {
        return status;
    }

    Long id() {
        return id;
    }

    List<String> riskReasons() {
        return List.copyOf(riskReasons);
    }

    TradingSignal toDomain() {
        return TradingSignal.restore(
                strategyName,
                stockCode,
                signalDate,
                signalType,
                score,
                reasons,
                riskReasons,
                status
        );
    }

    TradingSignalRecord toRecord() {
        return new TradingSignalRecord(
                id,
                strategyName,
                stockCode,
                signalDate,
                signalType,
                score,
                List.copyOf(reasons),
                List.copyOf(riskReasons),
                status
        );
    }
}
