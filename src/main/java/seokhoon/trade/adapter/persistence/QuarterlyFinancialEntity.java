package seokhoon.trade.adapter.persistence;

import jakarta.persistence.*;
import seokhoon.trade.domain.research.QuarterlyFinancial;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "quarterly_financials", uniqueConstraints = @UniqueConstraint(
        name = "uk_quarterly_financial_stock_quarter",
        columnNames = {"stock_code", "fiscal_year", "fiscal_quarter"}))
public class QuarterlyFinancialEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;
    @Column(name = "fiscal_year", nullable = false)
    private int fiscalYear;
    @Column(name = "fiscal_quarter", nullable = false)
    private int fiscalQuarter;
    @Column(name = "revenue", nullable = false, precision = 19, scale = 4)
    private BigDecimal revenue;
    @Column(name = "operating_income", nullable = false, precision = 19, scale = 4)
    private BigDecimal operatingIncome;
    @Column(name = "net_income", nullable = false, precision = 19, scale = 4)
    private BigDecimal netIncome;
    @Column(name = "total_assets", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAssets;
    @Column(name = "total_liabilities", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalLiabilities;
    @Column(name = "total_equity", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalEquity;
    @Column(name = "operating_cash_flow", nullable = false, precision = 19, scale = 4)
    private BigDecimal operatingCashFlow;
    @Column(name = "free_cash_flow", precision = 19, scale = 4)
    private BigDecimal freeCashFlow;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected QuarterlyFinancialEntity() {
    }

    static QuarterlyFinancialEntity from(QuarterlyFinancial value) {
        QuarterlyFinancialEntity entity = new QuarterlyFinancialEntity();
        entity.id = value.id();
        entity.update(value);
        return entity;
    }

    void update(QuarterlyFinancial value) {
        stockCode = value.stockCode();
        fiscalYear = value.fiscalYear();
        fiscalQuarter = value.fiscalQuarter();
        revenue = value.revenue();
        operatingIncome = value.operatingIncome();
        netIncome = value.netIncome();
        totalAssets = value.totalAssets();
        totalLiabilities = value.totalLiabilities();
        totalEquity = value.totalEquity();
        operatingCashFlow = value.operatingCashFlow();
        freeCashFlow = value.freeCashFlow();
        createdAt = value.createdAt();
        updatedAt = value.updatedAt();
    }

    QuarterlyFinancial toDomain() {
        return new QuarterlyFinancial(id, stockCode, fiscalYear, fiscalQuarter, revenue,
                operatingIncome, netIncome, totalAssets, totalLiabilities, totalEquity,
                operatingCashFlow, freeCashFlow, createdAt, updatedAt);
    }
}
