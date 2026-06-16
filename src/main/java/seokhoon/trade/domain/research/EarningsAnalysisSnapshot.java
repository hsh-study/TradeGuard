package seokhoon.trade.domain.research;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record EarningsAnalysisSnapshot(
        Long id,
        String stockCode,
        LocalDate baseDate,
        BigDecimal revenueYoyGrowth,
        BigDecimal operatingIncomeYoyGrowth,
        BigDecimal netIncomeYoyGrowth,
        BigDecimal operatingMargin,
        BigDecimal netMargin,
        BigDecimal debtRatio,
        BigDecimal operatingCashFlow,
        BigDecimal freeCashFlow,
        BigDecimal per,
        BigDecimal pbr,
        BigDecimal psr,
        Integer earningsQualityScore,
        Integer valuationScore,
        Integer overallScore,
        EarningsAnalysisStatus status,
        List<String> reasons,
        Instant createdAt,
        Instant updatedAt
) {
    public EarningsAnalysisSnapshot {
        Objects.requireNonNull(stockCode, "stockCode");
        Objects.requireNonNull(baseDate, "baseDate");
        Objects.requireNonNull(status, "status");
        reasons = List.copyOf(Objects.requireNonNull(reasons, "reasons"));
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }
}
