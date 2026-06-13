package seokhoon.trade.domain.position;

import java.math.BigDecimal;
import java.time.Instant;

public record LivePositionExitRule(
        Long id,
        long positionId,
        BigDecimal takeProfitRate,
        BigDecimal stopLossRate,
        BigDecimal maxLossAmount,
        BigDecimal sellTaxRate,
        BigDecimal buyCommissionRate,
        BigDecimal sellCommissionRate,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
}
