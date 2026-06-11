package seokhoon.trade.adapter.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import seokhoon.trade.application.port.in.EarlyMarketReportDataCompleteness;
import seokhoon.trade.application.port.in.EarlyMarketStrategyBacktestPeriodSummary;
import seokhoon.trade.application.port.in.EarlyMarketStrategyBacktestResult;
import seokhoon.trade.application.service.EarlyMarketStrategyExperimentNoDataException;
import seokhoon.trade.domain.strategy.EarlyMarketStrategyExperiment;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EarlyMarketStrategyBacktestControllerTest {
    @Test
    void runsBacktestWithPartialOverrides() throws Exception {
        EarlyMarketStrategyBacktestController controller =
                new EarlyMarketStrategyBacktestController(command -> {
                    assertThat(command.parameterOverrides().opening()
                            .entryThreshold()).isEqualTo(80);
                    assertThat(command.parameterOverrides().preOpen()).isNull();
                    return result();
                });

        mockMvc(controller).perform(post("/api/reports/early-market/backtests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "experimentName": "threshold 80",
                                  "from": "2026-06-01",
                                  "to": "2026-06-10",
                                  "parameterOverrides": {
                                    "opening": {
                                      "entryThreshold": 80
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.experiment.id").value(1))
                .andExpect(jsonPath(
                        "$.experiment.parameterSnapshot.opening.entryThreshold"
                ).value(80))
                .andExpect(jsonPath(
                        "$.periodReportSummary.candidateCount"
                ).value(3))
                .andExpect(jsonPath("$.warnings[0]")
                        .value("STORED_SIGNALS_NOT_RECALCULATED"));
    }

    @Test
    void returnsNotFoundWhenBacktestPeriodHasNoData() throws Exception {
        EarlyMarketStrategyBacktestController controller =
                new EarlyMarketStrategyBacktestController(command -> {
                    throw new EarlyMarketStrategyExperimentNoDataException(
                            command.from(),
                            command.to()
                    );
                });

        mockMvc(controller).perform(post("/api/reports/early-market/backtests")
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

    private static MockMvc mockMvc(
            EarlyMarketStrategyBacktestController controller
    ) {
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static EarlyMarketStrategyBacktestResult result() {
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 6, 10);
        EarlyMarketStrategyExperiment experiment =
                new EarlyMarketStrategyExperiment(
                        1L,
                        "threshold 80",
                        from,
                        to,
                        Map.of("opening", Map.of("entryThreshold", 80)),
                        3,
                        2,
                        new BigDecimal("1.5000"),
                        new BigDecimal("-1.0000"),
                        new BigDecimal("50.0000"),
                        11L,
                        12L,
                        Instant.parse("2026-06-11T02:00:00Z")
                );
        EarlyMarketReportDataCompleteness completeness =
                new EarlyMarketReportDataCompleteness(3, 2, 1, 2, 2, 2, 1);
        return new EarlyMarketStrategyBacktestResult(
                experiment,
                new EarlyMarketStrategyBacktestPeriodSummary(
                        from,
                        to,
                        2,
                        3,
                        2,
                        1,
                        new BigDecimal("1.5000"),
                        new BigDecimal("-1.0000"),
                        new BigDecimal("50.0000"),
                        11L,
                        12L,
                        completeness
                ),
                List.of(
                        "STORED_SIGNALS_NOT_RECALCULATED",
                        "PARAMETER_EFFECT_LIMITED_TO_REPORTING",
                        "MISSING_PERFORMANCE_ROWS"
                )
        );
    }
}
