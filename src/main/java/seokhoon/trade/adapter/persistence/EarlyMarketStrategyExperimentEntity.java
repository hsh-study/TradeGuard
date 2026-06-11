package seokhoon.trade.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import seokhoon.trade.domain.strategy.EarlyMarketStrategyExperiment;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

@Entity
@Table(name = "early_market_strategy_experiments")
public class EarlyMarketStrategyExperimentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "experiment_name", nullable = false, length = 100)
    private String experimentName;
    @Column(name = "from_date", nullable = false)
    private LocalDate from;
    @Column(name = "to_date", nullable = false)
    private LocalDate to;
    @Convert(converter = ObjectMapJsonConverter.class)
    @Column(name = "parameter_snapshot_json", nullable = false, columnDefinition = "TEXT")
    private Map<String, Object> parameterSnapshot;
    @Column(name = "candidate_count", nullable = false)
    private int candidateCount;
    @Column(name = "performance_captured_count", nullable = false)
    private int performanceCapturedCount;
    @Column(name = "average_max_return_rate", precision = 19, scale = 4)
    private BigDecimal averageMaxReturnRate;
    @Column(name = "average_max_drawdown_rate", precision = 19, scale = 4)
    private BigDecimal averageMaxDrawdownRate;
    @Column(name = "win_rate", precision = 19, scale = 4)
    private BigDecimal winRate;
    @Column(name = "best_signal_id")
    private Long bestSignalId;
    @Column(name = "worst_signal_id")
    private Long worstSignalId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected EarlyMarketStrategyExperimentEntity() {
    }

    static EarlyMarketStrategyExperimentEntity from(
            EarlyMarketStrategyExperiment experiment
    ) {
        EarlyMarketStrategyExperimentEntity entity =
                new EarlyMarketStrategyExperimentEntity();
        entity.experimentName = experiment.experimentName();
        entity.from = experiment.from();
        entity.to = experiment.to();
        entity.parameterSnapshot = experiment.parameterSnapshot();
        entity.candidateCount = experiment.candidateCount();
        entity.performanceCapturedCount = experiment.performanceCapturedCount();
        entity.averageMaxReturnRate = experiment.averageMaxReturnRate();
        entity.averageMaxDrawdownRate = experiment.averageMaxDrawdownRate();
        entity.winRate = experiment.winRate();
        entity.bestSignalId = experiment.bestSignalId();
        entity.worstSignalId = experiment.worstSignalId();
        entity.createdAt = experiment.createdAt();
        return entity;
    }

    EarlyMarketStrategyExperiment toDomain() {
        return new EarlyMarketStrategyExperiment(
                id,
                experimentName,
                from,
                to,
                parameterSnapshot,
                candidateCount,
                performanceCapturedCount,
                averageMaxReturnRate,
                averageMaxDrawdownRate,
                winRate,
                bestSignalId,
                worstSignalId,
                createdAt
        );
    }
}
