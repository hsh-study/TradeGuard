package seokhoon.trade.adapter.persistence;

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
import seokhoon.trade.domain.strategy.SignalType;
import seokhoon.trade.domain.strategy.TradingSignal;
import seokhoon.trade.domain.strategy.TradingSignalStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trading_signals")
public class TradingSignalEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String strategyName;
    private String stockCode;
    private LocalDate signalDate;
    @Enumerated(EnumType.STRING)
    private SignalType signalType;
    private int score;
    @ElementCollection
    @CollectionTable(name = "trading_signal_reasons", joinColumns = @JoinColumn(name = "trading_signal_id"))
    private List<String> reasons = new ArrayList<>();
    @Enumerated(EnumType.STRING)
    private TradingSignalStatus status;

    protected TradingSignalEntity() {
    }

    public static TradingSignalEntity from(TradingSignal signal) {
        TradingSignalEntity entity = new TradingSignalEntity();
        entity.strategyName = signal.strategyName();
        entity.stockCode = signal.stockCode();
        entity.signalDate = signal.signalDate();
        entity.signalType = signal.signalType();
        entity.score = signal.score();
        entity.reasons = new ArrayList<>(signal.reasons());
        entity.status = signal.status();
        return entity;
    }
}
