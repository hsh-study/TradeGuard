package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.strategy.TradingSignalStatus;
import seokhoon.trade.domain.audit.AuditActor;

import java.time.Instant;

public record SignalStatusHistoryRecord(
        Long id,
        long tradingSignalId,
        TradingSignalStatus fromStatus,
        TradingSignalStatus toStatus,
        String reason,
        AuditActor actor,
        String requestCorrelationId,
        Instant createdAt
) {
}
