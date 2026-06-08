package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.strategy.TradingSignalStatus;

import java.util.List;

public record ClosingBetPreScanCandidate(
        Long signalId,
        String strategyName,
        String stockCode,
        int score,
        List<String> reasons,
        List<String> riskReasons,
        TradingSignalStatus status
) {
}
