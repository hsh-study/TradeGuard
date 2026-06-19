package seokhoon.trade.adapter.persistence;

import jakarta.persistence.*;
import seokhoon.trade.domain.research.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "replay_backtest_results")
public class ReplayBacktestResultEntity {
    private static final String SEPARATOR = "\u001f";
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "run_id", nullable = false) private Long runId;
    @Column(name = "trade_date", nullable = false) private LocalDate tradeDate;
    @Column(name = "stock_code", nullable = false, length = 20) private String stockCode;
    @Column(name = "stock_name", nullable = false, length = 100) private String stockName;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private ReplayBacktestStrategy strategy;
    @Column(name = "candidate_rank", nullable = false) private int candidateRank;
    @Column(nullable = false) private int score;
    @Column(nullable = false, columnDefinition = "TEXT") private String reasons;
    @Column(nullable = false, columnDefinition = "TEXT") private String warnings;
    @Column(name = "entry_reference_price", precision = 19, scale = 4) private BigDecimal entryReferencePrice;
    @Column(name = "exit_reference_price", precision = 19, scale = 4) private BigDecimal exitReferencePrice;
    @Column(name = "holding_days") private Integer holdingDays;
    @Column(name = "return_rate", precision = 19, scale = 6) private BigDecimal returnRate;
    @Enumerated(EnumType.STRING) @Column(name = "result_status", nullable = false, length = 30)
    private ReplayBacktestResultStatus resultStatus;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected ReplayBacktestResultEntity() { }

    static ReplayBacktestResultEntity from(ReplayBacktestResult result) {
        ReplayBacktestResultEntity entity = new ReplayBacktestResultEntity();
        entity.id = result.id(); entity.runId = result.runId(); entity.tradeDate = result.tradeDate();
        entity.stockCode = result.stockCode(); entity.stockName = result.stockName(); entity.strategy = result.strategy();
        entity.candidateRank = result.candidateRank(); entity.score = result.score();
        entity.reasons = String.join(SEPARATOR, result.reasons()); entity.warnings = String.join(SEPARATOR, result.warnings());
        entity.entryReferencePrice = result.entryReferencePrice(); entity.exitReferencePrice = result.exitReferencePrice();
        entity.holdingDays = result.holdingDays(); entity.returnRate = result.returnRate();
        entity.resultStatus = result.resultStatus(); entity.createdAt = result.createdAt();
        return entity;
    }

    ReplayBacktestResult toDomain() {
        return new ReplayBacktestResult(id, runId, tradeDate, stockCode, stockName, strategy, candidateRank, score,
                split(reasons), split(warnings), entryReferencePrice, exitReferencePrice, holdingDays,
                returnRate, resultStatus, createdAt);
    }

    private static List<String> split(String value) {
        return value == null || value.isEmpty() ? List.of() : List.of(value.split(SEPARATOR, -1));
    }
}
