package seokhoon.trade.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import seokhoon.trade.application.port.in.CompareEarlyMarketStrategyExperimentsUseCase;
import seokhoon.trade.application.port.in.EarlyMarketStrategyExperimentComparison;
import seokhoon.trade.application.port.in.EarlyMarketStrategyExperimentComparisonItem;
import seokhoon.trade.application.port.out.EarlyMarketStrategyExperimentPort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.domain.strategy.EarlyMarketStrategyExperiment;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

@Service
public class EarlyMarketStrategyExperimentComparisonService
        implements CompareEarlyMarketStrategyExperimentsUseCase {
    private static final Logger log = LoggerFactory.getLogger(
            EarlyMarketStrategyExperimentComparisonService.class
    );
    private static final int MIN_EXPERIMENT_COUNT = 2;
    private static final int MAX_EXPERIMENT_COUNT = 10;

    private final EarlyMarketStrategyExperimentPort experimentPort;
    private final OperationalMetricsPort metricsPort;
    private final Clock clock;

    @Autowired
    public EarlyMarketStrategyExperimentComparisonService(
            EarlyMarketStrategyExperimentPort experimentPort,
            OperationalMetricsPort metricsPort
    ) {
        this(experimentPort, metricsPort, Clock.systemUTC());
    }

    EarlyMarketStrategyExperimentComparisonService(
            EarlyMarketStrategyExperimentPort experimentPort,
            OperationalMetricsPort metricsPort,
            Clock clock
    ) {
        this.experimentPort = experimentPort;
        this.metricsPort = metricsPort;
        this.clock = clock;
    }

    @Override
    public EarlyMarketStrategyExperimentComparison compare(
            List<Long> experimentIds
    ) {
        try {
            List<Long> ids = validateIds(experimentIds);
            List<EarlyMarketStrategyExperimentComparisonItem> experiments =
                    ids.stream()
                            .map(this::load)
                            .map(EarlyMarketStrategyExperimentComparisonService::toItem)
                            .toList();
            EarlyMarketStrategyExperimentComparison comparison =
                    new EarlyMarketStrategyExperimentComparison(
                            ids,
                            clock.instant(),
                            experiments,
                            best(experiments,
                                    EarlyMarketStrategyExperimentComparisonItem::winRate),
                            best(experiments,
                                    EarlyMarketStrategyExperimentComparisonItem::averageMaxReturnRate),
                            best(experiments,
                                    EarlyMarketStrategyExperimentComparisonItem::averageMaxDrawdownRate),
                            notes(experiments)
                    );
            metricsPort.recordEarlyMarketExperimentCompare("success");
            log.atInfo()
                    .addKeyValue("experimentCount", experiments.size())
                    .addKeyValue("noteCount", comparison.notes().size())
                    .addKeyValue("result", "success")
                    .log("Early market strategy experiments compared");
            return comparison;
        } catch (RuntimeException exception) {
            metricsPort.recordEarlyMarketExperimentCompare("failure");
            log.atError()
                    .addKeyValue(
                            "experimentCount",
                            experimentIds == null ? 0 : experimentIds.size()
                    )
                    .addKeyValue("result", "failure")
                    .setCause(exception)
                    .log("Early market strategy experiment comparison failed");
            throw exception;
        }
    }

    private EarlyMarketStrategyExperiment load(long id) {
        return experimentPort.findById(id)
                .orElseThrow(() ->
                        new EarlyMarketStrategyExperimentNotFoundException(id));
    }

    private static List<Long> validateIds(List<Long> experimentIds) {
        if (experimentIds == null) {
            throw new IllegalArgumentException("ids are required");
        }
        if (experimentIds.size() < MIN_EXPERIMENT_COUNT
                || experimentIds.size() > MAX_EXPERIMENT_COUNT) {
            throw new IllegalArgumentException(
                    "ids must contain between 2 and 10 experiment ids"
            );
        }
        if (experimentIds.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("ids must not contain null");
        }
        if (experimentIds.stream().anyMatch(id -> id < 1)) {
            throw new IllegalArgumentException(
                    "experiment id must be at least 1"
            );
        }
        Set<Long> uniqueIds = new HashSet<>(experimentIds);
        if (uniqueIds.size() != experimentIds.size()) {
            throw new IllegalArgumentException(
                    "ids must not contain duplicates"
            );
        }
        return List.copyOf(experimentIds);
    }

    private static EarlyMarketStrategyExperimentComparisonItem best(
            List<EarlyMarketStrategyExperimentComparisonItem> experiments,
            Function<EarlyMarketStrategyExperimentComparisonItem, BigDecimal> extractor
    ) {
        EarlyMarketStrategyExperimentComparisonItem best = null;
        BigDecimal bestValue = null;
        for (EarlyMarketStrategyExperimentComparisonItem experiment : experiments) {
            BigDecimal value = extractor.apply(experiment);
            if (value != null
                    && (bestValue == null || value.compareTo(bestValue) > 0)) {
                best = experiment;
                bestValue = value;
            }
        }
        return best;
    }

    private static List<String> notes(
            List<EarlyMarketStrategyExperimentComparisonItem> experiments
    ) {
        List<String> notes = new ArrayList<>();
        long periodCount = experiments.stream()
                .map(experiment -> experiment.from() + "/" + experiment.to())
                .distinct()
                .count();
        if (periodCount > 1) {
            notes.add("DIFFERENT_PERIODS");
        }
        int minCandidateCount = experiments.stream()
                .mapToInt(EarlyMarketStrategyExperimentComparisonItem::candidateCount)
                .min()
                .orElseThrow();
        int maxCandidateCount = experiments.stream()
                .mapToInt(EarlyMarketStrategyExperimentComparisonItem::candidateCount)
                .max()
                .orElseThrow();
        boolean differentSampleSize = minCandidateCount == 0
                ? maxCandidateCount > 0
                : maxCandidateCount >= minCandidateCount * 2L;
        if (differentSampleSize) {
            notes.add("DIFFERENT_SAMPLE_SIZE");
        }
        return List.copyOf(notes);
    }

    private static EarlyMarketStrategyExperimentComparisonItem toItem(
            EarlyMarketStrategyExperiment experiment
    ) {
        return new EarlyMarketStrategyExperimentComparisonItem(
                experiment.id(),
                experiment.experimentName(),
                experiment.from(),
                experiment.to(),
                experiment.candidateCount(),
                experiment.performanceCapturedCount(),
                experiment.averageMaxReturnRate(),
                experiment.averageMaxDrawdownRate(),
                experiment.winRate(),
                experiment.bestSignalId(),
                experiment.worstSignalId(),
                experiment.parameterSnapshot()
        );
    }
}
