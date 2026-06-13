package seokhoon.trade.domain.position;

import java.math.BigDecimal;
import java.time.Instant;

public record LivePosition(
        Long id,
        String stockCode,
        int quantity,
        BigDecimal averageBuyPrice,
        BigDecimal buyAmount,
        BigDecimal buyCommission,
        LivePositionStatus status,
        Instant openedAt,
        Instant closedAt
) {
    public LivePosition withStatus(LivePositionStatus next, Instant time) {
        return new LivePosition(
                id, stockCode, quantity, averageBuyPrice, buyAmount,
                buyCommission, next, openedAt,
                next == LivePositionStatus.CLOSED ? time : closedAt
        );
    }
}
