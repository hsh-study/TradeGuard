package seokhoon.trade.domain.risk;

import org.junit.jupiter.api.Test;
import seokhoon.trade.domain.order.OrderRequest;
import seokhoon.trade.domain.order.OrderSide;
import seokhoon.trade.domain.order.OrderType;
import seokhoon.trade.domain.strategy.SignalType;
import seokhoon.trade.domain.strategy.TradingSignal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RiskManagerTest {
    private final RiskManager riskManager = new RiskManager();

    @Test
    void rejectsSignalBelow70() {
        TradingSignal signal = signal(69);
        RiskDecision decision = riskManager.evaluate(signal, order(1, "50000"), (stockCode, strategyName, tradeDate, side) -> false);

        assertThat(decision.approved()).isFalse();
        assertThat(decision.reasons()).contains("SCORE_BELOW_70");
        assertThat(signal.riskReasons()).containsExactly("SCORE_BELOW_70");
    }

    @Test
    void rejectsDuplicateOrder() {
        RiskDecision decision = riskManager.evaluate(signal(80), order(1, "50000"), (stockCode, strategyName, tradeDate, side) -> true);

        assertThat(decision.approved()).isFalse();
        assertThat(decision.reasons()).contains("DUPLICATE_ORDER");
    }

    @Test
    void rejectsOrderAmountOverLimit() {
        RiskDecision decision = riskManager.evaluate(signal(80), order(2, "60000"), (stockCode, strategyName, tradeDate, side) -> false);

        assertThat(decision.approved()).isFalse();
        assertThat(decision.reasons()).contains("ORDER_AMOUNT_EXCEEDS_LIMIT");
    }

    @Test
    void approvesValidLimitOrder() {
        TradingSignal signal = signal(80);
        RiskDecision decision = riskManager.evaluate(signal, order(1, "50000"), (stockCode, strategyName, tradeDate, side) -> false);

        assertThat(decision.approved()).isTrue();
        assertThat(signal.status().name()).isEqualTo("RISK_APPROVED");
        assertThat(signal.riskReasons()).isEmpty();
    }

    @Test
    void approvesEarlyMarketEntryCandidateButRejectsPreScanSignal() {
        TradingSignal entry = new TradingSignal(
                "EARLY_MARKET_BREAKOUT",
                "005930",
                LocalDate.of(2026, 6, 10),
                SignalType.EARLY_MARKET_ENTRY_CANDIDATE,
                80,
                List.of("ABOVE_VWAP")
        );
        TradingSignal preScan = new TradingSignal(
                "EARLY_MARKET_BREAKOUT",
                "000660",
                LocalDate.of(2026, 6, 10),
                SignalType.EARLY_MARKET_PRE_SCAN,
                80,
                List.of("TRADING_VALUE_TOP")
        );

        RiskDecision entryDecision = riskManager.evaluate(
                entry,
                new OrderRequest(
                        "005930",
                        OrderSide.BUY,
                        OrderType.LIMIT,
                        1,
                        BigDecimal.valueOf(50_000),
                        "EARLY_MARKET_BREAKOUT",
                        LocalDate.of(2026, 6, 10)
                ),
                (stockCode, strategyName, tradeDate, side) -> false
        );
        RiskDecision preScanDecision = riskManager.evaluate(
                preScan,
                new OrderRequest(
                        "000660",
                        OrderSide.BUY,
                        OrderType.LIMIT,
                        1,
                        BigDecimal.valueOf(50_000),
                        "EARLY_MARKET_BREAKOUT",
                        LocalDate.of(2026, 6, 10)
                ),
                (stockCode, strategyName, tradeDate, side) -> false
        );

        assertThat(entryDecision.approved()).isTrue();
        assertThat(preScanDecision.approved()).isFalse();
        assertThat(preScanDecision.reasons())
                .contains("ONLY_BUY_CANDIDATE_SUPPORTED_IN_MVP");
    }

    @Test
    void storesAllRejectionReasonsOnSignal() {
        TradingSignal signal = signal(69);

        RiskDecision decision = riskManager.evaluate(
                signal,
                order(3, "50000"),
                (stockCode, strategyName, tradeDate, side) -> true
        );

        assertThat(signal.status().name()).isEqualTo("RISK_REJECTED");
        assertThat(signal.riskReasons()).containsExactlyElementsOf(decision.reasons());
        assertThat(signal.riskReasons())
                .contains("SCORE_BELOW_70", "ORDER_AMOUNT_EXCEEDS_LIMIT", "DUPLICATE_ORDER");
    }

    private static TradingSignal signal(int score) {
        return new TradingSignal("CLOSING_BET", "005930", LocalDate.of(2026, 2, 1), SignalType.BUY_CANDIDATE, score, List.of("TEST"));
    }

    private static OrderRequest order(int quantity, String limitPrice) {
        return new OrderRequest("005930", OrderSide.BUY, OrderType.LIMIT, quantity, new BigDecimal(limitPrice), "CLOSING_BET", LocalDate.of(2026, 2, 1));
    }
}
