package seokhoon.trade.domain.research;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record EarningsPreview(
        Long id,
        long earningsEventId,
        String stockCode,
        LocalDate previewDate,
        List<String> keyCheckpoints,
        BigDecimal expectedRevenue,
        BigDecimal expectedOperatingIncome,
        BigDecimal expectedNetIncome,
        BigDecimal expectedOperatingMargin,
        List<String> expectedRisks,
        List<String> thesisWatchPoints,
        EarningsPreviewStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public EarningsPreview {
        Objects.requireNonNull(stockCode, "stockCode");
        Objects.requireNonNull(previewDate, "previewDate");
        keyCheckpoints = List.copyOf(Objects.requireNonNull(keyCheckpoints, "keyCheckpoints"));
        expectedRisks = List.copyOf(Objects.requireNonNull(expectedRisks, "expectedRisks"));
        thesisWatchPoints = List.copyOf(Objects.requireNonNull(thesisWatchPoints, "thesisWatchPoints"));
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }
}
