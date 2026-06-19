package seokhoon.trade.adapter.persistence;

import jakarta.persistence.*;
import seokhoon.trade.domain.research.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "paper_trading_report_results")
public class PaperTradingReportResultEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "run_id", nullable = false) private Long runId;
    @Column(name = "trade_date", nullable = false) private LocalDate tradeDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private PaperTradingStrategy strategy;
    @Column(name = "stock_code", nullable = false, length = 20) private String stockCode;
    @Column(name = "stock_name", nullable = false, length = 100) private String stockName;
    @Column(name = "candidate_rank", nullable = false) private int candidateRank;
    @Column(name = "signal_id") private Long signalId;
    @Column(nullable = false) private int score;
    @Convert(converter = StringListJsonConverter.class) @Column(nullable = false, columnDefinition = "TEXT") private List<String> reasons = List.of();
    @Convert(converter = StringListJsonConverter.class) @Column(nullable = false, columnDefinition = "TEXT") private List<String> warnings = List.of();
    @Column(name = "reference_entry_price", precision = 19, scale = 4) private BigDecimal referenceEntryPrice;
    @Column(name = "reference_exit_price", precision = 19, scale = 4) private BigDecimal referenceExitPrice;
    @Column(name = "high_after_entry", precision = 19, scale = 4) private BigDecimal highAfterEntry;
    @Column(name = "low_after_entry", precision = 19, scale = 4) private BigDecimal lowAfterEntry;
    @Column(name = "max_favorable_excursion", precision = 19, scale = 6) private BigDecimal maxFavorableExcursion;
    @Column(name = "max_adverse_excursion", precision = 19, scale = 6) private BigDecimal maxAdverseExcursion;
    @Column(name = "return_rate", precision = 19, scale = 6) private BigDecimal returnRate;
    @Enumerated(EnumType.STRING) @Column(name = "result_status", nullable = false, length = 30) private PaperTradingResultStatus resultStatus;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected PaperTradingReportResultEntity() { }
    static PaperTradingReportResultEntity from(PaperTradingReportResult value) {
        PaperTradingReportResultEntity entity = new PaperTradingReportResultEntity();
        entity.id=value.id(); entity.runId=value.runId(); entity.tradeDate=value.tradeDate(); entity.strategy=value.strategy();
        entity.stockCode=value.stockCode(); entity.stockName=value.stockName(); entity.candidateRank=value.candidateRank(); entity.signalId=value.signalId();
        entity.score=value.score(); entity.reasons=List.copyOf(value.reasons()); entity.warnings=List.copyOf(value.warnings());
        entity.referenceEntryPrice=value.referenceEntryPrice(); entity.referenceExitPrice=value.referenceExitPrice();
        entity.highAfterEntry=value.highAfterEntry(); entity.lowAfterEntry=value.lowAfterEntry();
        entity.maxFavorableExcursion=value.maxFavorableExcursion(); entity.maxAdverseExcursion=value.maxAdverseExcursion();
        entity.returnRate=value.returnRate(); entity.resultStatus=value.resultStatus(); entity.createdAt=value.createdAt(); return entity;
    }
    PaperTradingReportResult toDomain() {
        return new PaperTradingReportResult(id, runId, tradeDate, strategy, stockCode, stockName, candidateRank,
                signalId, score, List.copyOf(reasons), List.copyOf(warnings), referenceEntryPrice, referenceExitPrice,
                highAfterEntry, lowAfterEntry, maxFavorableExcursion, maxAdverseExcursion, returnRate, resultStatus, createdAt);
    }
}
