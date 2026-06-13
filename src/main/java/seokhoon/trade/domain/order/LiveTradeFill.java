package seokhoon.trade.domain.order;

import java.math.BigDecimal;
import java.time.Instant;

public record LiveTradeFill(
        Long id,
        long liveOrderRequestId,
        String stockCode,
        OrderSide side,
        int filledQuantity,
        BigDecimal filledPrice,
        BigDecimal filledAmount,
        BigDecimal fee,
        BigDecimal tax,
        Instant filledAt
) {
}
