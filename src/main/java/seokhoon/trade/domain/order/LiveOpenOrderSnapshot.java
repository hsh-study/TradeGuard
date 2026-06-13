package seokhoon.trade.domain.order;

import java.math.BigDecimal;
import java.time.Instant;

public record LiveOpenOrderSnapshot(
        long liveOrderRequestId,
        int filledQuantity,
        int remainingQuantity,
        BigDecimal averageFilledPrice,
        Instant inquiredAt
) {}
