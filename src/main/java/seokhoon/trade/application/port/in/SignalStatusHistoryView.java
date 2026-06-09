package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.strategy.TradingSignalStatus;

import java.time.Instant;

public record SignalStatusHistoryView(
        Long historyId,
        long signalId,
        TradingSignalStatus fromStatus,
        TradingSignalStatus toStatus,
        String reason,
        Instant createdAt
) {
}
