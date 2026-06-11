package seokhoon.trade.application.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import seokhoon.trade.adapter.metrics.MicrometerOperationalMetricsAdapter;
import seokhoon.trade.application.port.in.CreateEarlyMarketStrategyExperimentCommand;
import seokhoon.trade.application.port.in.EarlyMarketReportDataCompleteness;
import seokhoon.trade.application.port.in.EarlyMarketStrategyCandidateReport;
import seokhoon.trade.application.port.in.EarlyMarketStrategyPeriodReport;
import seokhoon.trade.application.port.out.EarlyMarketStrategyExperimentPort;
import seokhoon.trade.config.EarlyMarketStrategyProperties;
import seokhoon.trade.domain.strategy.EarlyMarketStrategyExperiment;
import seokhoon.trade.domain.strategy.SignalType;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EarlyMarketStrategyExperimentServiceTest {
    private static final LocalDate FROM = LocalDate.of(2026, 6, 1);
    private static final LocalDate TO = LocalDate.of(2026, 6, 10);
    private static final Instant CREATED_AT =
            Instant.parse("2026-06-11T00:00:00Z");

    @Test
    void savesPeriodReportWithCurrentParameterSnapshot() {
        RecordingPort port = new RecordingPort();
        EarlyMarketStrategyProperties properties =
                new EarlyMarketStrategyProperties();
        properties.getOpening().setEntryThreshold(80);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        EarlyMarketStrategyExperimentService service = service(
                periodReport(3),
                port,
                properties,
                registry
        );

        EarlyMarketStrategyExperiment saved = service.create(
                new CreateEarlyMarketStrategyExperimentCommand(
                        "entry threshold 80",
                        FROM,
                        TO
                )
        );

        assertThat(saved.id()).isEqualTo(1L);
        assertThat(saved.candidateCount()).isEqualTo(3);
        assertThat(saved.performanceCapturedCount()).isEqualTo(2);
        assertThat(saved.bestSignalId()).isEqualTo(11L);
        assertThat(saved.worstSignalId()).isEqualTo(12L);
        assertThat(saved.createdAt()).isEqualTo(CREATED_AT);
        assertThat(saved.parameterSnapshot()).containsKeys(
                "preOpen",
                "opening",
                "followUp",
                "priceAction"
        );
        assertThat(castMap(saved.parameterSnapshot().get("opening")))
                .containsEntry("entryThreshold", 80)
                .containsEntry("maxCandidates", 3);
        assertThat(castMap(saved.parameterSnapshot().get("followUp")))
                .containsEntry(
                        "excludeDrawdownFromHigh",
                        new BigDecimal("-2.0")
                );
        assertThat(registry.find("tradeguard.early_market.experiment.count")
                .tag("result", "saved")
                .counter().count()).isEqualTo(1.0);
    }

    @Test
    void doesNotSaveExperimentWhenPeriodHasNoData() {
        RecordingPort port = new RecordingPort();
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        EarlyMarketStrategyExperimentService service = service(
                periodReport(0),
                port,
                new EarlyMarketStrategyProperties(),
                registry
        );

        assertThatThrownBy(() -> service.create(
                new CreateEarlyMarketStrategyExperimentCommand(
                        "empty period",
                        FROM,
                        TO
                )
        ))
                .isInstanceOf(EarlyMarketStrategyExperimentNoDataException.class);
        assertThat(port.values).isEmpty();
        assertThat(registry.find("tradeguard.early_market.experiment.count")
                .tag("result", "no_data")
                .counter().count()).isEqualTo(1.0);
    }

    @Test
    void loadsRecentExperimentsAndSingleExperiment() {
        RecordingPort port = new RecordingPort();
        EarlyMarketStrategyExperimentService service = service(
                periodReport(1),
                port,
                new EarlyMarketStrategyProperties(),
                new SimpleMeterRegistry()
        );
        EarlyMarketStrategyExperiment first = service.create(
                new CreateEarlyMarketStrategyExperimentCommand("first", FROM, TO)
        );
        EarlyMarketStrategyExperiment second = service.create(
                new CreateEarlyMarketStrategyExperimentCommand("second", FROM, TO)
        );

        assertThat(service.findById(first.id()).experimentName())
                .isEqualTo("first");
        assertThat(service.findRecent(1))
                .extracting(EarlyMarketStrategyExperiment::id)
                .containsExactly(second.id());
    }

    private static EarlyMarketStrategyExperimentService service(
            EarlyMarketStrategyPeriodReport report,
            RecordingPort port,
            EarlyMarketStrategyProperties properties,
            SimpleMeterRegistry registry
    ) {
        return new EarlyMarketStrategyExperimentService(
                (from, to) -> report,
                port,
                properties,
                new MicrometerOperationalMetricsAdapter(registry),
                Clock.fixed(CREATED_AT, ZoneOffset.UTC)
        );
    }

    private static EarlyMarketStrategyPeriodReport periodReport(
            int candidateCount
    ) {
        EarlyMarketStrategyCandidateReport best = candidateCount == 0
                ? null
                : candidate(11L, "5.0");
        EarlyMarketStrategyCandidateReport worst = candidateCount == 0
                ? null
                : candidate(12L, "-2.0");
        int capturedCount = candidateCount == 0 ? 0 : 2;
        return new EarlyMarketStrategyPeriodReport(
                FROM,
                TO,
                candidateCount == 0 ? 0 : 2,
                candidateCount,
                capturedCount,
                candidateCount - capturedCount,
                candidateCount == 0 ? null : new BigDecimal("1.5000"),
                candidateCount == 0 ? null : new BigDecimal("-1.0000"),
                candidateCount == 0 ? null : new BigDecimal("50.0000"),
                best,
                worst,
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                new EarlyMarketReportDataCompleteness(
                        candidateCount,
                        capturedCount,
                        candidateCount - capturedCount,
                        capturedCount,
                        capturedCount,
                        capturedCount,
                        capturedCount == 0 ? 0 : 1
                )
        );
    }

    private static EarlyMarketStrategyCandidateReport candidate(
            long signalId,
            String maxReturn
    ) {
        return new EarlyMarketStrategyCandidateReport(
                signalId,
                TO,
                "STOCK" + signalId,
                SignalType.EARLY_MARKET_ENTRY_CANDIDATE,
                90,
                null,
                new BigDecimal(maxReturn),
                new BigDecimal("-1.0"),
                false,
                List.of(),
                List.of()
        );
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    private static class RecordingPort
            implements EarlyMarketStrategyExperimentPort {
        private final List<EarlyMarketStrategyExperiment> values =
                new ArrayList<>();

        @Override
        public EarlyMarketStrategyExperiment save(
                EarlyMarketStrategyExperiment experiment
        ) {
            EarlyMarketStrategyExperiment saved =
                    new EarlyMarketStrategyExperiment(
                            (long) values.size() + 1,
                            experiment.experimentName(),
                            experiment.from(),
                            experiment.to(),
                            experiment.parameterSnapshot(),
                            experiment.candidateCount(),
                            experiment.performanceCapturedCount(),
                            experiment.averageMaxReturnRate(),
                            experiment.averageMaxDrawdownRate(),
                            experiment.winRate(),
                            experiment.bestSignalId(),
                            experiment.worstSignalId(),
                            experiment.createdAt()
                    );
            values.add(saved);
            return saved;
        }

        @Override
        public Optional<EarlyMarketStrategyExperiment> findById(long id) {
            return values.stream()
                    .filter(value -> value.id() == id)
                    .findFirst();
        }

        @Override
        public List<EarlyMarketStrategyExperiment> findRecent(int limit) {
            return values.reversed().stream().limit(limit).toList();
        }
    }
}
