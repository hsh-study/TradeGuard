package seokhoon.trade.adapter.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.out.EarlyMarketStrategyExperimentPort;
import seokhoon.trade.domain.strategy.EarlyMarketStrategyExperiment;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class EarlyMarketStrategyExperimentPersistenceIntegrationTest {
    @Autowired
    private EarlyMarketStrategyExperimentPort experimentPort;

    @Test
    void persistsParameterSnapshotAndLoadsRecentExperiments() {
        EarlyMarketStrategyExperiment first = experimentPort.save(experiment(
                "first",
                Instant.parse("2026-06-11T00:00:00Z"),
                70
        ));
        EarlyMarketStrategyExperiment second = experimentPort.save(experiment(
                "second",
                Instant.parse("2026-06-11T01:00:00Z"),
                80
        ));

        assertThat(experimentPort.findById(first.id()))
                .hasValueSatisfying(value -> {
                    assertThat(value.experimentName()).isEqualTo("first");
                    assertThat(castMap(value.parameterSnapshot().get("opening")))
                            .containsEntry("entryThreshold", 70);
                    assertThat(castMap(value.parameterSnapshot().get("followUp")))
                            .containsEntry(
                                    "excludeDrawdownFromHigh",
                                    new BigDecimal("-2.0")
                            );
                });
        assertThat(experimentPort.findRecent(1))
                .extracting(EarlyMarketStrategyExperiment::id)
                .containsExactly(second.id());
    }

    private static EarlyMarketStrategyExperiment experiment(
            String name,
            Instant createdAt,
            int entryThreshold
    ) {
        return new EarlyMarketStrategyExperiment(
                null,
                name,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 10),
                Map.of(
                        "opening", Map.of(
                                "entryThreshold", entryThreshold,
                                "maxCandidates", 3
                        ),
                        "followUp", Map.of(
                                "excludeDrawdownFromHigh",
                                new BigDecimal("-2.0")
                        )
                ),
                3,
                2,
                new BigDecimal("1.5000"),
                new BigDecimal("-1.0000"),
                new BigDecimal("50.0000"),
                11L,
                12L,
                createdAt
        );
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }
}
