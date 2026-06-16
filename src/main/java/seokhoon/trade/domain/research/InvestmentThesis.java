package seokhoon.trade.domain.research;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record InvestmentThesis(
        Long id,
        String stockCode,
        String title,
        String coreAssumption,
        String invalidationCondition,
        BigDecimal targetPrice,
        String stopLossCondition,
        int confidence,
        ThesisStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public InvestmentThesis {
        requireText(stockCode, "stockCode");
        requireText(title, "title");
        requireText(coreAssumption, "coreAssumption");
        requireText(invalidationCondition, "invalidationCondition");
        requireText(stopLossCondition, "stopLossCondition");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if (targetPrice != null && targetPrice.signum() < 0) {
            throw new IllegalArgumentException("targetPrice must not be negative");
        }
        if (confidence < 0 || confidence > 100) {
            throw new IllegalArgumentException("confidence must be between 0 and 100");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
