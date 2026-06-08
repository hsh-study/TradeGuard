package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.strategy.TradingSignalStatus;

import java.util.List;

public record ClosingBetFinalReviewCandidate(
        Long preScanSignalId,
        Long finalSignalId,
        String strategyName,
        String stockCode,
        int score,
        List<String> reasons,
        List<String> riskReasons,
        TradingSignalStatus status
) {
}
