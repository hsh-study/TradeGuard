package seokhoon.trade.domain.research;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record PostEarningsReview(
        Long id,
        long earningsEventId,
        String stockCode,
        LocalDate reviewDate,
        BigDecimal actualRevenue,
        BigDecimal actualOperatingIncome,
        BigDecimal actualNetIncome,
        BigDecimal actualOperatingMargin,
        BigDecimal revenueSurpriseRate,
        BigDecimal operatingIncomeSurpriseRate,
        ThesisImpact thesisImpact,
        String reviewSummary,
        List<String> actionItems,
        Instant createdAt,
        Instant updatedAt
) {
    public PostEarningsReview {
        Objects.requireNonNull(stockCode, "stockCode");
        Objects.requireNonNull(reviewDate, "reviewDate");
        Objects.requireNonNull(actualRevenue, "actualRevenue");
        Objects.requireNonNull(actualOperatingIncome, "actualOperatingIncome");
        Objects.requireNonNull(actualNetIncome, "actualNetIncome");
        Objects.requireNonNull(thesisImpact, "thesisImpact");
        Objects.requireNonNull(reviewSummary, "reviewSummary");
        actionItems = List.copyOf(Objects.requireNonNull(actionItems, "actionItems"));
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }
}
