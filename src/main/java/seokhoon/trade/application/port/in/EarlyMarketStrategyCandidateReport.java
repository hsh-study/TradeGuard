package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.strategy.SignalType;

import java.math.BigDecimal;
import java.util.List;

public record EarlyMarketStrategyCandidateReport(
        long signalId,
        String stockCode,
        SignalType signalType,
        int signalScore,
        EarlyMarketFollowUpDecision followUpDecision,
        BigDecimal maxReturnRateUntil0930,
        BigDecimal maxDrawdownRateUntil0930,
        Boolean vwapBroken,
        List<String> reasons,
        List<String> riskReasons
) {
}
