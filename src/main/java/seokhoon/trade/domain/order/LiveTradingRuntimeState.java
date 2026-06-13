package seokhoon.trade.domain.order;

import java.time.Instant;

public record LiveTradingRuntimeState(
        boolean killSwitchEnabled,
        String reason,
        Instant updatedAt
) {
}
