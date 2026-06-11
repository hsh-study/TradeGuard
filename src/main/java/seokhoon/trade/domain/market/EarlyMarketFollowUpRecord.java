package seokhoon.trade.domain.market;

import seokhoon.trade.application.port.in.EarlyMarketFollowUpDecision;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record EarlyMarketFollowUpRecord(
        long signalId,
        LocalDate tradeDate,
        String stockCode,
        EarlyMarketFollowUpDecision decision,
        int signalScore,
        BigDecimal lastPrice,
        BigDecimal highSince0905,
        BigDecimal drawdownFromHigh,
        Boolean vwapBroken,
        List<String> reasons,
        Instant capturedAt
) {
}
