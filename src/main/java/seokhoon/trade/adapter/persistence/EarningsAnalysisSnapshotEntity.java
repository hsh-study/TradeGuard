package seokhoon.trade.adapter.persistence;

import jakarta.persistence.*;
import seokhoon.trade.domain.research.EarningsAnalysisSnapshot;
import seokhoon.trade.domain.research.EarningsAnalysisStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "earnings_analysis_snapshots", uniqueConstraints = @UniqueConstraint(
        name = "uk_earnings_analysis_stock_date", columnNames = {"stock_code", "base_date"}))
public class EarningsAnalysisSnapshotEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;
    @Column(name = "base_date", nullable = false)
    private LocalDate baseDate;
    @Column(name = "revenue_yoy_growth", precision = 19, scale = 4)
    private BigDecimal revenueYoyGrowth;
    @Column(name = "operating_income_yoy_growth", precision = 19, scale = 4)
    private BigDecimal operatingIncomeYoyGrowth;
    @Column(name = "net_income_yoy_growth", precision = 19, scale = 4)
    private BigDecimal netIncomeYoyGrowth;
    @Column(name = "operating_margin", precision = 19, scale = 4)
    private BigDecimal operatingMargin;
    @Column(name = "net_margin", precision = 19, scale = 4)
    private BigDecimal netMargin;
    @Column(name = "debt_ratio", precision = 19, scale = 4)
    private BigDecimal debtRatio;
    @Column(name = "operating_cash_flow", precision = 19, scale = 4)
    private BigDecimal operatingCashFlow;
    @Column(name = "free_cash_flow", precision = 19, scale = 4)
    private BigDecimal freeCashFlow;
    @Column(name = "per", precision = 19, scale = 4)
    private BigDecimal per;
    @Column(name = "pbr", precision = 19, scale = 4)
    private BigDecimal pbr;
    @Column(name = "psr", precision = 19, scale = 4)
    private BigDecimal psr;
    @Column(name = "earnings_quality_score")
    private Integer earningsQualityScore;
    @Column(name = "valuation_score")
    private Integer valuationScore;
    @Column(name = "overall_score")
    private Integer overallScore;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private EarningsAnalysisStatus status;
    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "reasons", nullable = false, columnDefinition = "TEXT")
    private List<String> reasons;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected EarningsAnalysisSnapshotEntity() {
    }

    static EarningsAnalysisSnapshotEntity from(EarningsAnalysisSnapshot value) {
        EarningsAnalysisSnapshotEntity entity = new EarningsAnalysisSnapshotEntity();
        entity.id = value.id();
        entity.update(value);
        return entity;
    }

    void update(EarningsAnalysisSnapshot value) {
        stockCode = value.stockCode();
        baseDate = value.baseDate();
        revenueYoyGrowth = value.revenueYoyGrowth();
        operatingIncomeYoyGrowth = value.operatingIncomeYoyGrowth();
        netIncomeYoyGrowth = value.netIncomeYoyGrowth();
        operatingMargin = value.operatingMargin();
        netMargin = value.netMargin();
        debtRatio = value.debtRatio();
        operatingCashFlow = value.operatingCashFlow();
        freeCashFlow = value.freeCashFlow();
        per = value.per();
        pbr = value.pbr();
        psr = value.psr();
        earningsQualityScore = value.earningsQualityScore();
        valuationScore = value.valuationScore();
        overallScore = value.overallScore();
        status = value.status();
        reasons = value.reasons();
        createdAt = value.createdAt();
        updatedAt = value.updatedAt();
    }

    EarningsAnalysisSnapshot toDomain() {
        return new EarningsAnalysisSnapshot(id, stockCode, baseDate, revenueYoyGrowth,
                operatingIncomeYoyGrowth, netIncomeYoyGrowth, operatingMargin, netMargin,
                debtRatio, operatingCashFlow, freeCashFlow, per, pbr, psr,
                earningsQualityScore, valuationScore, overallScore, status, reasons,
                createdAt, updatedAt);
    }
}
