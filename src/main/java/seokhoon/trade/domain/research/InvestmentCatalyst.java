package seokhoon.trade.domain.research;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public record InvestmentCatalyst(
        Long id,
        String stockCode,
        String title,
        CatalystType catalystType,
        LocalDate expectedDate,
        CatalystImportance importance,
        CatalystStatus status,
        String sourceUrl,
        String memo,
        Instant createdAt,
        Instant updatedAt
) {
    public InvestmentCatalyst {
        requireText(title, "title");
        Objects.requireNonNull(catalystType, "catalystType");
        Objects.requireNonNull(expectedDate, "expectedDate");
        Objects.requireNonNull(importance, "importance");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if (stockCode != null && stockCode.isBlank()) {
            throw new IllegalArgumentException("stockCode must not be blank");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
