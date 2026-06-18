package seokhoon.trade.adapter.persistence;

import jakarta.persistence.*;
import seokhoon.trade.domain.market.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name="investor_flow_import_histories")
class InvestorFlowImportHistoryEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) InvestorFlowImportScope scope;
    @Column(name="stock_code", length=20) String stockCode;
    @Enumerated(EnumType.STRING) @Column(length=20) InvestorFlowMarket market;
    @Column(name="trade_date", nullable=false) LocalDate tradeDate;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) InvestorFlowProvider provider;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) InvestorFlowImportStatus status;
    @Column(name="imported_count", nullable=false) int importedCount;
    @Column(name="failure_reason", length=1000) String failureReason;
    @Column(name="requested_at", nullable=false) Instant requestedAt;
    @Column(name="completed_at", nullable=false) Instant completedAt;
    static InvestorFlowImportHistoryEntity from(InvestorFlowImportHistory v) { var e=new InvestorFlowImportHistoryEntity();
        e.scope=v.scope(); e.stockCode=v.stockCode(); e.market=v.market(); e.tradeDate=v.tradeDate(); e.provider=v.provider();
        e.status=v.status(); e.importedCount=v.importedCount(); e.failureReason=v.failureReason(); e.requestedAt=v.requestedAt(); e.completedAt=v.completedAt(); return e; }
    InvestorFlowImportHistory toDomain() { return new InvestorFlowImportHistory(id, scope, stockCode, market,
            tradeDate, provider, status, importedCount, failureReason, requestedAt, completedAt); }
}
