package seokhoon.trade.domain.risk;

import seokhoon.trade.domain.order.OrderRequest;
import seokhoon.trade.domain.order.OrderSide;
import seokhoon.trade.domain.order.OrderType;
import seokhoon.trade.domain.strategy.SignalType;
import seokhoon.trade.domain.strategy.TradingSignal;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class RiskManager {
    private static final int MINIMUM_SCORE = 70;
    private final BigDecimal maxOrderAmount;

    public RiskManager() {
        this(BigDecimal.valueOf(100_000));
    }

    public RiskManager(BigDecimal maxOrderAmount) {
        this.maxOrderAmount = maxOrderAmount;
    }

    public RiskDecision evaluate(TradingSignal signal, OrderRequest orderRequest, ExistingOrderLookup existingOrderLookup) {
        List<String> reasons = new ArrayList<>();
        if (signal.score() < MINIMUM_SCORE) {
            reasons.add("SCORE_BELOW_70");
        }
        boolean supportedBuySignal = signal.signalType() == SignalType.BUY_CANDIDATE
                || signal.signalType() == SignalType.EARLY_MARKET_ENTRY_CANDIDATE;
        if (!supportedBuySignal || orderRequest.side() != OrderSide.BUY) {
            reasons.add("ONLY_BUY_CANDIDATE_SUPPORTED_IN_MVP");
        }
        if (orderRequest.orderType() != OrderType.LIMIT) {
            reasons.add("ONLY_LIMIT_ORDER_ALLOWED");
        }
        if (orderRequest.quantity() < 1) {
            reasons.add("QUANTITY_LESS_THAN_ONE");
        }
        if (orderRequest.orderAmount().compareTo(maxOrderAmount) > 0) {
            reasons.add("ORDER_AMOUNT_EXCEEDS_LIMIT");
        }
        if (existingOrderLookup.exists(orderRequest.stockCode(), orderRequest.strategyName(), orderRequest.tradeDate(), orderRequest.side())) {
            reasons.add("DUPLICATE_ORDER");
        }
        if (reasons.isEmpty()) {
            signal.approveRisk();
            return RiskDecision.approve();
        }
        signal.rejectRisk(reasons);
        return RiskDecision.rejected(reasons);
    }
}
