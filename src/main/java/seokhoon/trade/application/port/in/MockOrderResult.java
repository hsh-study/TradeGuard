package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.order.OrderRequest;
import seokhoon.trade.domain.risk.RiskDecision;
import seokhoon.trade.domain.strategy.TradingSignal;

public record MockOrderResult(
        RiskDecision riskDecision,
        TradingSignal tradingSignal,
        OrderRequest orderRequest,
        boolean brokerFailed,
        String failureReason
) {
    public static MockOrderResult rejected(RiskDecision decision, TradingSignal signal) {
        return new MockOrderResult(decision, signal, null, false, null);
    }

    public static MockOrderResult accepted(
            RiskDecision decision,
            TradingSignal signal,
            OrderRequest orderRequest
    ) {
        return new MockOrderResult(decision, signal, orderRequest, false, null);
    }

    public static MockOrderResult brokerFailed(
            RiskDecision decision,
            TradingSignal signal,
            OrderRequest orderRequest
    ) {
        return new MockOrderResult(
                decision,
                signal,
                orderRequest,
                true,
                orderRequest.failureReason()
        );
    }
}
