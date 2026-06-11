package seokhoon.trade.adapter.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import seokhoon.trade.application.port.in.CreateEarlyMarketStrategyExperimentCommand;
import seokhoon.trade.application.port.in.CreateEarlyMarketStrategyExperimentUseCase;
import seokhoon.trade.application.port.in.EarlyMarketStrategyExperimentComparison;
import seokhoon.trade.application.port.in.EarlyMarketStrategyExperimentComparisonItem;
import seokhoon.trade.application.port.in.LoadEarlyMarketStrategyExperimentsUseCase;
import seokhoon.trade.application.service.EarlyMarketStrategyExperimentNoDataException;
import seokhoon.trade.application.service.EarlyMarketStrategyExperimentNotFoundException;
import seokhoon.trade.domain.strategy.EarlyMarketStrategyExperiment;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EarlyMarketStrategyExperimentControllerTest {
    @Test
    void createsAndLoadsExperiments() throws Exception {
        EarlyMarketStrategyExperiment experiment = experiment();
        EarlyMarketStrategyExperimentController controller =
                new EarlyMarketStrategyExperimentController(
                        command -> experiment,
                        loadUseCase(experiment),
                        ids -> comparison(experiment)
                );
        MockMvc mockMvc = mockMvc(controller);

        mockMvc.perform(post("/api/reports/early-market/experiments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "experimentName": "threshold 80",
                                  "from": "2026-06-01",
                                  "to": "2026-06-10"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.parameterSnapshot.opening.entryThreshold")
                        .value(80));

        mockMvc.perform(get("/api/reports/early-market/experiments")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].experimentName")
                        .value("threshold 80"));

        mockMvc.perform(get("/api/reports/early-market/experiments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateCount").value(3));
    }

    @Test
    void returnsNotFoundWhenExperimentPeriodHasNoData() throws Exception {
        CreateEarlyMarketStrategyExperimentUseCase createUseCase = command -> {
            throw new EarlyMarketStrategyExperimentNoDataException(
                    command.from(),
                    command.to()
            );
        };
        EarlyMarketStrategyExperimentController controller =
                new EarlyMarketStrategyExperimentController(
                        createUseCase,
                        loadUseCase(experiment()),
                        ids -> comparison(experiment())
                );

        mockMvc(controller).perform(
                        post("/api/reports/early-market/experiments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "experimentName": "empty",
                                          "from": "2026-06-01",
                                          "to": "2026-06-10"
                                        }
                                        """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(
                        "EARLY_MARKET_STRATEGY_EXPERIMENT_NO_DATA"
                ));
    }

    @Test
    void comparesCommaSeparatedExperimentIds() throws Exception {
        EarlyMarketStrategyExperiment experiment = experiment();
        EarlyMarketStrategyExperimentController controller =
                new EarlyMarketStrategyExperimentController(
                        command -> experiment,
                        loadUseCase(experiment),
                        ids -> {
                            org.assertj.core.api.Assertions.assertThat(ids)
                                    .containsExactly(1L, 2L, 3L);
                            return comparison(experiment);
                        }
                );

        mockMvc(controller).perform(
                        get("/api/reports/early-market/experiments/compare")
                                .param("ids", "1,2,3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.experimentIds[0]").value(1))
                .andExpect(jsonPath("$.bestByWinRate.id").value(1))
                .andExpect(jsonPath("$.notes[0]").value("DIFFERENT_PERIODS"));
    }

    @Test
    void validatesCompareIdsAndReturnsNotFoundForMissingExperiment()
            throws Exception {
        EarlyMarketStrategyExperiment experiment = experiment();
        EarlyMarketStrategyExperimentController controller =
                new EarlyMarketStrategyExperimentController(
                        command -> experiment,
                        loadUseCase(experiment),
                        ids -> {
                            throw new EarlyMarketStrategyExperimentNotFoundException(
                                    99L
                            );
                        }
                );
        MockMvc mockMvc = mockMvc(controller);

        mockMvc.perform(get("/api/reports/early-market/experiments/compare"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/reports/early-market/experiments/compare")
                        .param("ids", "1,99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(
                        "EARLY_MARKET_STRATEGY_EXPERIMENT_NOT_FOUND"
                ));
    }

    private static MockMvc mockMvc(
            EarlyMarketStrategyExperimentController controller
    ) {
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static LoadEarlyMarketStrategyExperimentsUseCase loadUseCase(
            EarlyMarketStrategyExperiment experiment
    ) {
        return new LoadEarlyMarketStrategyExperimentsUseCase() {
            @Override
            public EarlyMarketStrategyExperiment findById(long id) {
                return experiment;
            }

            @Override
            public List<EarlyMarketStrategyExperiment> findRecent(int limit) {
                return List.of(experiment);
            }
        };
    }

    private static EarlyMarketStrategyExperiment experiment() {
        return new EarlyMarketStrategyExperiment(
                1L,
                "threshold 80",
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 10),
                Map.of("opening", Map.of("entryThreshold", 80)),
                3,
                2,
                new BigDecimal("1.5000"),
                new BigDecimal("-1.0000"),
                new BigDecimal("50.0000"),
                11L,
                12L,
                Instant.parse("2026-06-11T00:00:00Z")
        );
    }

    private static EarlyMarketStrategyExperimentComparison comparison(
            EarlyMarketStrategyExperiment experiment
    ) {
        EarlyMarketStrategyExperimentComparisonItem item =
                new EarlyMarketStrategyExperimentComparisonItem(
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
        return new EarlyMarketStrategyExperimentComparison(
                List.of(1L, 2L, 3L),
                Instant.parse("2026-06-11T01:00:00Z"),
                List.of(item),
                item,
                item,
                item,
                List.of("DIFFERENT_PERIODS")
        );
    }
}
