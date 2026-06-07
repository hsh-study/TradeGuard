package seokhoon.trade.domain.strategy;

import java.time.LocalDate;
import java.util.List;

public class TradingSignal {
    private final String strategyName;
    private final String stockCode;
    private final LocalDate signalDate;
    private final SignalType signalType;
    private final int score;
    private final List<String> reasons;
    private List<String> riskReasons;
    private TradingSignalStatus status;

    public TradingSignal(String strategyName, String stockCode, LocalDate signalDate, SignalType signalType, int score, List<String> reasons) {
        this.strategyName = strategyName;
        this.stockCode = stockCode;
        this.signalDate = signalDate;
        this.signalType = signalType;
        this.score = score;
        this.reasons = List.copyOf(reasons);
        this.riskReasons = List.of();
        this.status = TradingSignalStatus.CREATED;
    }

    public static TradingSignal restore(
            String strategyName,
            String stockCode,
            LocalDate signalDate,
            SignalType signalType,
            int score,
            List<String> reasons,
            List<String> riskReasons,
            TradingSignalStatus status
    ) {
        TradingSignal signal = new TradingSignal(
                strategyName,
                stockCode,
                signalDate,
                signalType,
                score,
                reasons
        );
        signal.riskReasons = List.copyOf(riskReasons);
        signal.status = status;
        return signal;
    }

    public String strategyName() { return strategyName; }
    public String stockCode() { return stockCode; }
    public LocalDate signalDate() { return signalDate; }
    public SignalType signalType() { return signalType; }
    public int score() { return score; }
    public List<String> reasons() { return reasons; }
    public List<String> riskReasons() { return riskReasons; }
    public TradingSignalStatus status() { return status; }
    public void approveRisk() {
        this.riskReasons = List.of();
        this.status = TradingSignalStatus.RISK_APPROVED;
    }
    public void rejectRisk(List<String> reasons) {
        this.riskReasons = List.copyOf(reasons);
        this.status = TradingSignalStatus.RISK_REJECTED;
    }
    public void markOrderRequested() { this.status = TradingSignalStatus.ORDER_REQUESTED; }
}
