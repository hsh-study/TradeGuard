package seokhoon.trade.adapter.web;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.EarlyMarketReportDataCompleteness;
import seokhoon.trade.application.port.in.EarlyMarketStrategyCandidateReport;
import seokhoon.trade.application.port.in.EarlyMarketStrategyDailyReport;
import seokhoon.trade.application.port.in.EarlyMarketStrategyGroupReport;
import seokhoon.trade.domain.strategy.SignalType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EarlyMarketStrategyReportControllerTest {
    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 6, 10);

    @Test
    void loadsDailyEarlyMarketStrategyReport() {
        EarlyMarketStrategyReportController controller =
                new EarlyMarketStrategyReportController(
                        tradeDate -> report()
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

    private static EarlyMarketStrategyDailyReport report() {
        EarlyMarketStrategyCandidateReport candidate =
                new EarlyMarketStrategyCandidateReport(
                        2L,
                        "005930",
                        SignalType.EARLY_MARKET_ENTRY_CANDIDATE,
                        95,
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
                new EarlyMarketReportDataCompleteness(2, 2, 0, 2, 2),
                List.of(candidate)
        );
    }
}
