package seokhoon.trade.domain.research;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record QuarterlyFinancial(
        Long id,
        String stockCode,
        int fiscalYear,
        int fiscalQuarter,
        BigDecimal revenue,
        BigDecimal operatingIncome,
        BigDecimal netIncome,
        BigDecimal totalAssets,
        BigDecimal totalLiabilities,
        BigDecimal totalEquity,
        BigDecimal operatingCashFlow,
        BigDecimal freeCashFlow,
        Instant createdAt,
        Instant updatedAt
) {
    public QuarterlyFinancial {
        Objects.requireNonNull(stockCode, "stockCode");
        Objects.requireNonNull(revenue, "revenue");
        Objects.requireNonNull(operatingIncome, "operatingIncome");
        Objects.requireNonNull(netIncome, "netIncome");
        Objects.requireNonNull(totalAssets, "totalAssets");
        Objects.requireNonNull(totalLiabilities, "totalLiabilities");
        Objects.requireNonNull(totalEquity, "totalEquity");
        Objects.requireNonNull(operatingCashFlow, "operatingCashFlow");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if (fiscalQuarter < 1 || fiscalQuarter > 4) {
            throw new IllegalArgumentException("fiscalQuarter must be 1..4");
        }
    }
}
