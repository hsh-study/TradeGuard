package seokhoon.trade.application.port.in;

import java.math.BigDecimal;
import java.util.List;

public record EarlyMarketFollowUpCandidate(
        Long signalId,
        String stockCode,
        int signalScore,
        EarlyMarketFollowUpDecision decision,
        List<String> reasons,
        BigDecimal lastPrice,
        BigDecimal highSince0905,
        BigDecimal drawdownFromHigh,
        Boolean vwapBroken
) {
}
