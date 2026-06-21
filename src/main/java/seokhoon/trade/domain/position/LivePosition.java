package seokhoon.trade.domain.position;

import java.math.BigDecimal;
import java.time.Instant;
import seokhoon.trade.domain.kis.KisEnvironment;

public record LivePosition(
        Long id,
        String stockCode,
        KisEnvironment environment,
        int quantity,
        BigDecimal averageBuyPrice,
        BigDecimal buyAmount,
        BigDecimal buyCommission,
        LivePositionStatus status,
        Instant openedAt,
        Instant closedAt
) {
    public LivePosition(Long id, String stockCode, int quantity,
            BigDecimal averageBuyPrice, BigDecimal buyAmount,
            BigDecimal buyCommission, LivePositionStatus status,
            Instant openedAt, Instant closedAt) {
        this(id, stockCode, null, quantity, averageBuyPrice, buyAmount,
                buyCommission, status, openedAt, closedAt);
    }

    public LivePosition withStatus(LivePositionStatus next, Instant time) {
        return new LivePosition(
                id, stockCode, environment, quantity, averageBuyPrice, buyAmount,
                buyCommission, next, openedAt,
                next == LivePositionStatus.CLOSED ? time : closedAt
        );
    }
}
