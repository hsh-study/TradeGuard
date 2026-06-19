package seokhoon.trade.adapter.persistence;

import jakarta.persistence.*;
import seokhoon.trade.domain.research.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "replay_backtest_runs")
public class ReplayBacktestRunEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private ReplayBacktestStrategy strategy;
    @Column(name = "from_date", nullable = false) private LocalDate fromDate;
    @Column(name = "to_date", nullable = false) private LocalDate toDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private ReplayBacktestStatus status;
    @Column(name = "parameter_snapshot", nullable = false, columnDefinition = "TEXT")
    private String parameterSnapshot;
    @Column(name = "total_candidates", nullable = false) private int totalCandidates;
    @Column(name = "win_count", nullable = false) private int winCount;
    @Column(name = "loss_count", nullable = false) private int lossCount;
    @Column(name = "average_return_rate", precision = 19, scale = 6) private BigDecimal averageReturnRate;
    @Column(name = "max_return_rate", precision = 19, scale = 6) private BigDecimal maxReturnRate;
    @Column(name = "min_return_rate", precision = 19, scale = 6) private BigDecimal minReturnRate;
    @Column(name = "failure_reason", length = 1000) private String failureReason;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "completed_at") private Instant completedAt;

    protected ReplayBacktestRunEntity() { }

    static ReplayBacktestRunEntity from(ReplayBacktestRun run) {
        ReplayBacktestRunEntity entity = new ReplayBacktestRunEntity();
        entity.id = run.id();
        entity.update(run);
        return entity;
    }

    void update(ReplayBacktestRun run) {
        strategy = run.strategy(); fromDate = run.fromDate(); toDate = run.toDate();
        status = run.status(); parameterSnapshot = run.parameterSnapshot();
        totalCandidates = run.totalCandidates(); winCount = run.winCount(); lossCount = run.lossCount();
        averageReturnRate = run.averageReturnRate(); maxReturnRate = run.maxReturnRate(); minReturnRate = run.minReturnRate();
        failureReason = run.failureReason(); createdAt = run.createdAt(); completedAt = run.completedAt();
    }

    ReplayBacktestRun toDomain() {
        return new ReplayBacktestRun(id, strategy, fromDate, toDate, status, parameterSnapshot,
                totalCandidates, winCount, lossCount, averageReturnRate, maxReturnRate, minReturnRate,
                failureReason, createdAt, completedAt);
    }
}
