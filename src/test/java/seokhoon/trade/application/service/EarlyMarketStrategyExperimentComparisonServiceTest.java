package seokhoon.trade.application.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import seokhoon.trade.adapter.metrics.MicrometerOperationalMetricsAdapter;
import seokhoon.trade.application.port.out.EarlyMarketStrategyExperimentPort;
import seokhoon.trade.domain.strategy.EarlyMarketStrategyExperiment;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EarlyMarketStrategyExperimentComparisonServiceTest {
    private static final LocalDate FROM = LocalDate.of(2026, 6, 1);
    private static final LocalDate TO = LocalDate.of(2026, 6, 10);
    private static final Instant COMPARED_AT =
            Instant.parse("2026-06-11T01:00:00Z");

    @Test
    void selectsBestExperimentsByEachMetric() {
        var service = service(List.of(
                experiment(1L, "best win", FROM, TO, 20, "60", "1.0", "-2.0"),
                experiment(2L, "best return", FROM, TO, 20, "50", "3.0", "-1.5"),
                experiment(3L, "best drawdown", FROM, TO, 20, "40", "2.0", "-0.8")
        ), new SimpleMeterRegistry());

        var comparison = service.compare(List.of(1L, 2L, 3L));

        assertThat(comparison.experimentIds()).containsExactly(1L, 2L, 3L);
        assertThat(comparison.comparedAt()).isEqualTo(COMPARED_AT);
        assertThat(comparison.bestByWinRate().id()).isEqualTo(1L);
        assertThat(comparison.bestByAverageMaxReturnRate().id()).isEqualTo(2L);
        assertThat(comparison.bestByAverageMaxDrawdownRate().id()).isEqualTo(3L);
        assertThat(comparison.experiments().getFirst().parameterSnapshot())
                .containsKey("opening");
        assertThat(comparison.notes()).isEmpty();
    }

    @Test
    void addsNotesForDifferentPeriodsAndLargeSampleSizeDifference() {
        var service = service(List.of(
                experiment(1L, "small", FROM, TO, 10, "50", "1", "-1"),
                experiment(
                        2L,
                        "large",
                        FROM.minusDays(10),
                        TO,
                        20,
                        "55",
                        "2",
                        "-2"
                )
        ), new SimpleMeterRegistry());

        var comparison = service.compare(List.of(1L, 2L));

        assertThat(comparison.notes()).containsExactly(
                "DIFFERENT_PERIODS",
                "DIFFERENT_SAMPLE_SIZE"
        );
    }

    @Test
    void validatesIdsAndRecordsFailureMetric() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        var service = service(List.of(
                experiment(1L, "one", FROM, TO, 10, "50", "1", "-1")
        ), registry);

        assertThatThrownBy(() -> service.compare(List.of(1L)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.compare(List.of(1L, 1L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ids must not contain duplicates");
        assertThatThrownBy(() -> service.compare(List.of(
                1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ids must contain between 2 and 10 experiment ids");
        assertThatThrownBy(() -> service.compare(List.of(1L, 2L)))
                .isInstanceOf(EarlyMarketStrategyExperimentNotFoundException.class);
        assertThat(registry.find(
                        "tradeguard.early_market.experiment.compare.count")
                .tag("result", "failure")
                .counter().count()).isEqualTo(4.0);
    }

    @Test
    void recordsSuccessMetricWithoutExperimentIdTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        var service = service(List.of(
                experiment(1L, "one", FROM, TO, 10, "50", "1", "-1"),
                experiment(2L, "two", FROM, TO, 11, "55", "2", "-2")
        ), registry);

        service.compare(List.of(1L, 2L));

        assertThat(registry.find(
                        "tradeguard.early_market.experiment.compare.count")
                .tag("result", "success")
                .counter().count()).isEqualTo(1.0);
        assertThat(registry.getMeters())
                .flatExtracting(meter -> meter.getId().getTags())
                .extracting(tag -> tag.getKey())
                .doesNotContain("experimentId", "experimentIds");
    }

    private static EarlyMarketStrategyExperimentComparisonService service(
            List<EarlyMarketStrategyExperiment> experiments,
            SimpleMeterRegistry registry
    ) {
        EarlyMarketStrategyExperimentPort port =
                new EarlyMarketStrategyExperimentPort() {
                    @Override
                    public EarlyMarketStrategyExperiment save(
                            EarlyMarketStrategyExperiment experiment
                    ) {
                        return experiment;
                    }

                    @Override
                    public Optional<EarlyMarketStrategyExperiment> findById(long id) {
                        return experiments.stream()
                                .filter(experiment -> experiment.id() == id)
                                .findFirst();
                    }

                    @Override
                    public List<EarlyMarketStrategyExperiment> findRecent(int limit) {
                        return experiments.stream().limit(limit).toList();
                    }
                };
        return new EarlyMarketStrategyExperimentComparisonService(
                port,
                new MicrometerOperationalMetricsAdapter(registry),
                Clock.fixed(COMPARED_AT, ZoneOffset.UTC)
        );
    }

    private static EarlyMarketStrategyExperiment experiment(
            long id,
            String name,
            LocalDate from,
            LocalDate to,
            int candidateCount,
            String winRate,
            String averageReturn,
            String averageDrawdown
    ) {
        return new EarlyMarketStrategyExperiment(
                id,
                name,
                from,
                to,
                Map.of("opening", Map.of("entryThreshold", 70 + (int) id)),
                candidateCount,
                candidateCount - 1,
                new BigDecimal(averageReturn),
                new BigDecimal(averageDrawdown),
                new BigDecimal(winRate),
                id * 10,
                id * 10 + 1,
                Instant.parse("2026-06-11T00:00:00Z")
        );
    }
}
