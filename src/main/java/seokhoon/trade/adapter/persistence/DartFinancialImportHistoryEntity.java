package seokhoon.trade.adapter.persistence;

import jakarta.persistence.*;
import seokhoon.trade.domain.research.DartFinancialImportHistory;
import seokhoon.trade.domain.research.DartFinancialImportStatus;

import java.time.Instant;

@Entity
@Table(name = "dart_financial_import_histories")
public class DartFinancialImportHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;
    @Column(name = "corp_code", length = 20)
    private String corpCode;
    @Column(name = "fiscal_year", nullable = false)
    private int fiscalYear;
    @Column(name = "report_code", nullable = false, length = 10)
    private String reportCode;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DartFinancialImportStatus status;
    @Column(name = "imported_quarterly_financial_count", nullable = false)
    private int importedQuarterlyFinancialCount;
    @Column(name = "failure_reason", length = 1000)
    private String failureReason;
    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;
    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    protected DartFinancialImportHistoryEntity() {
    }

    static DartFinancialImportHistoryEntity from(DartFinancialImportHistory value) {
        DartFinancialImportHistoryEntity entity = new DartFinancialImportHistoryEntity();
        entity.id = value.id();
        entity.update(value);
        return entity;
    }

    void update(DartFinancialImportHistory value) {
        stockCode = value.stockCode();
        corpCode = value.corpCode();
        fiscalYear = value.fiscalYear();
        reportCode = value.reportCode();
        status = value.status();
        importedQuarterlyFinancialCount = value.importedQuarterlyFinancialCount();
        failureReason = value.failureReason();
        requestedAt = value.requestedAt();
        completedAt = value.completedAt();
    }

    DartFinancialImportHistory toDomain() {
        return new DartFinancialImportHistory(id, stockCode, corpCode, fiscalYear,
                reportCode, status, importedQuarterlyFinancialCount, failureReason,
                requestedAt, completedAt);
    }
}
