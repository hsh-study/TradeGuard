package seokhoon.trade.adapter.web;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import seokhoon.trade.application.port.in.EarlyMarketReportDataCompleteness;
import seokhoon.trade.application.port.in.EarlyMarketFollowUpDecision;
import seokhoon.trade.application.port.in.EarlyMarketStrategyCandidateReport;
import seokhoon.trade.application.port.in.EarlyMarketStrategyDailyReport;
import seokhoon.trade.application.port.in.EarlyMarketStrategyGroupReport;
import seokhoon.trade.application.port.in.EarlyMarketStrategyPeriodReport;
import seokhoon.trade.domain.strategy.SignalType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EarlyMarketStrategyReportControllerTest {
    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 6, 10);

    @Test
    void loadsDailyEarlyMarketStrategyReport() {
        EarlyMarketStrategyReportController controller =
                new EarlyMarketStrategyReportController(
                        tradeDate -> report(),
                        (from, to) -> periodReport(from, to)
                );

        var response = controller.daily(TRADE_DATE);

        assertThat(response.tradeDate()).isEqualTo(TRADE_DATE);
        assertThat(response.preScanCount()).isEqualTo(1);
        assertThat(response.entryCandidateCount()).isEqualTo(1);
        assertThat(response.averageMaxReturnRate()).isEqualByComparingTo("6.5000");
        assertThat(response.bestCandidate().signalId()).isEqualTo(2L);
        assertThat(response.byScoreBucket()).containsKey("90+");
        assertThat(response.dataCompleteness().excludedFromPerformanceCount())
                .isZero();
    }

    @Test
    void loadsPeriodEarlyMarketStrategyReport() {
        EarlyMarketStrategyReportController controller =
                new EarlyMarketStrategyReportController(
                        tradeDate -> report(),
                        EarlyMarketStrategyReportControllerTest::periodReport
                );

        var response = controller.period(
                TRADE_DATE.minusDays(1),
                TRADE_DATE
        );

        assertThat(response.from()).isEqualTo(TRADE_DATE.minusDays(1));
        assertThat(response.to()).isEqualTo(TRADE_DATE);
        assertThat(response.tradingDayCount()).isEqualTo(1);
        assertThat(response.winRate()).isEqualByComparingTo("100.0000");
        assertThat(response.byFollowUpDecision()).containsKey("KEEP");
    }

    @Test
    void servesPeriodReportApiAndValidatesRequiredRange() throws Exception {
        EarlyMarketStrategyReportController controller =
                new EarlyMarketStrategyReportController(
                        tradeDate -> report(),
                        (from, to) -> {
                            if (from.isAfter(to)) {
                                throw new IllegalArgumentException(
                                        "from must be on or before to"
                                );
                            }
                            return periodReport(from, to);
                        }
                );
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/api/reports/early-market/period")
                        .param("from", "2026-06-09")
                        .param("to", "2026-06-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tradingDayCount").value(1))
                .andExpect(jsonPath("$.winRate").value(100.0000));

        mockMvc.perform(get("/api/reports/early-market/period")
                        .param("to", "2026-06-10"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/reports/early-market/period")
                        .param("from", "2026-06-10")
                        .param("to", "2026-06-09"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    private static EarlyMarketStrategyDailyReport report() {
        EarlyMarketStrategyCandidateReport candidate =
                new EarlyMarketStrategyCandidateReport(
                        2L,
                        TRADE_DATE,
                        "005930",
                        SignalType.EARLY_MARKET_ENTRY_CANDIDATE,
                        95,
                        EarlyMarketFollowUpDecision.KEEP,
                        new BigDecimal("6.5"),
                        new BigDecimal("-2.5"),
                        false,
                        List.of("PREVIOUS_HIGH_BROKEN", "OPENING_PRICE_HELD"),
                        List.of()
                );
        EarlyMarketStrategyGroupReport group =
                new EarlyMarketStrategyGroupReport(
                        1,
                        1,
                        new BigDecimal("6.5"),
                        new BigDecimal("-2.5")
                );
        return new EarlyMarketStrategyDailyReport(
                TRADE_DATE,
                1,
                1,
                2,
                0,
                new BigDecimal("6.5000"),
                new BigDecimal("-2.5000"),
                candidate,
                candidate,
                Map.of("EARLY_MARKET_ENTRY_CANDIDATE", group),
                Map.of("90+", group),
                Map.of("FALSE", group),
                Map.of("TRUE", group),
                Map.of("TRUE", group),
                Map.of("KEEP", group),
                new EarlyMarketReportDataCompleteness(2, 2, 0, 2, 2, 2, 2),
                List.of(candidate)
        );
    }

    private static EarlyMarketStrategyPeriodReport periodReport(
            LocalDate from,
            LocalDate to
    ) {
        var daily = report();
        return new EarlyMarketStrategyPeriodReport(
                from,
                to,
                1,
                2,
                2,
                0,
                daily.averageMaxReturnRate(),
                daily.averageMaxDrawdownRate(),
                new BigDecimal("100.0000"),
                daily.bestCandidate(),
                daily.worstCandidate(),
                Map.of(),
                daily.bySignalType(),
                daily.byScoreBucket(),
                daily.byVwapBroken(),
                daily.byPreviousHighBreakout(),
                daily.byOpeningPriceHeld(),
                daily.byFollowUpDecision(),
                daily.dataCompleteness()
        );
    }
}
