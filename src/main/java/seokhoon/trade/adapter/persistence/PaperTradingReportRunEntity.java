package seokhoon.trade.adapter.persistence;

import jakarta.persistence.*;
import seokhoon.trade.domain.research.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "paper_trading_report_runs")
public class PaperTradingReportRunEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "trade_date", nullable = false) private LocalDate tradeDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PaperTradingReportStatus status;
    @Column(name = "total_candidates", nullable = false) private int totalCandidates;
    @Column(name = "average_return_rate", precision = 19, scale = 6) private BigDecimal averageReturnRate;
    @Column(name = "win_count", nullable = false) private int winCount;
    @Column(name = "loss_count", nullable = false) private int lossCount;
    @Column(name = "flat_count", nullable = false) private int flatCount;
    @Column(name = "failure_reason", length = 1000) private String failureReason;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "completed_at") private Instant completedAt;

    protected PaperTradingReportRunEntity() { }

    static PaperTradingReportRunEntity from(PaperTradingReportRun run) {
        PaperTradingReportRunEntity entity = new PaperTradingReportRunEntity(); entity.id = run.id(); entity.update(run); return entity;
    }
    void update(PaperTradingReportRun run) {
        tradeDate = run.tradeDate(); status = run.status(); totalCandidates = run.totalCandidates();
        averageReturnRate = run.averageReturnRate(); winCount = run.winCount(); lossCount = run.lossCount();
        flatCount = run.flatCount(); failureReason = run.failureReason(); createdAt = run.createdAt(); completedAt = run.completedAt();
    }
    PaperTradingReportRun toDomain() {
        return new PaperTradingReportRun(id, tradeDate, status, totalCandidates, averageReturnRate,
                winCount, lossCount, flatCount, failureReason, createdAt, completedAt);
    }
}
