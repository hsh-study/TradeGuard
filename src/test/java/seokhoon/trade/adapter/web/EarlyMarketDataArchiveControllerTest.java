package seokhoon.trade.adapter.web;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import seokhoon.trade.application.port.in.LoadEarlyMarketDataArchiveUseCase;
import seokhoon.trade.domain.market.BarInterval;
import seokhoon.trade.domain.market.EarlyMarketAfterHoursSnapshot;
import seokhoon.trade.domain.market.EarlyMarketCaptureStatus;
import seokhoon.trade.domain.market.EarlyMarketCaptureType;
import seokhoon.trade.domain.market.EarlyMarketDataCapture;
import seokhoon.trade.domain.market.EarlyMarketIntradayBarSnapshot;
import seokhoon.trade.domain.market.EarlyMarketMarketSnapshot;
import seokhoon.trade.domain.market.EarlyMarketRankingSnapshot;
import seokhoon.trade.domain.market.EarlyMarketSnapshotType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EarlyMarketDataArchiveControllerTest {
    private static final LocalDate DATE = LocalDate.of(2026, 6, 10);
    private static final Instant AT = Instant.parse("2026-06-10T00:05:00Z");

    @Test
    void exposesAllArchiveQueries() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new EarlyMarketDataArchiveController(useCase())
        ).build();

        mvc.perform(get("/api/early-market/data-captures")
                        .param("tradeDate", DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("SUCCEEDED"));
        mvc.perform(get("/api/early-market/ranking-snapshots")
                        .param("tradeDate", DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stockCode").value("005930"));
        mvc.perform(get("/api/early-market/after-hours-snapshots")
                        .param("tradeDate", DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].previousTradingDay")
                        .value("2026-06-09"));
        mvc.perform(get("/api/early-market/intraday-bars")
                        .param("tradeDate", DATE.toString())
                        .param("stockCode", "005930"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].barTime").value("09:01:00"));
        mvc.perform(get("/api/early-market/market-snapshots")
                        .param("tradeDate", DATE.toString())
                        .param("stockCode", "005930"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].snapshotType")
                        .value("OPENING_0905"));
    }

    private static LoadEarlyMarketDataArchiveUseCase useCase() {
        return new LoadEarlyMarketDataArchiveUseCase() {
            @Override
            public List<EarlyMarketDataCapture> loadCaptures(LocalDate date) {
                return List.of(new EarlyMarketDataCapture(
                        1L, date, EarlyMarketCaptureType.OPENING_BARS_0900_0930,
                        AT, "TEST", EarlyMarketCaptureStatus.SUCCEEDED,
                        1, null, AT
                ));
            }

            @Override
            public List<EarlyMarketRankingSnapshot> loadRankings(LocalDate date) {
                return List.of(new EarlyMarketRankingSnapshot(
                        1L, date, AT, 1, "005930", "삼성전자",
                        amount(100), amount(1), 10, amount(1000), "TEST"
                ));
            }

            @Override
            public List<EarlyMarketAfterHoursSnapshot> loadAfterHours(
                    LocalDate date
            ) {
                return List.of(new EarlyMarketAfterHoursSnapshot(
                        1L, date, date.minusDays(1), AT, "005930",
                        amount(100), amount(1), 10, amount(1000), "TEST"
                ));
            }

            @Override
            public List<EarlyMarketIntradayBarSnapshot> loadBars(
                    LocalDate date,
                    String stockCode
            ) {
                return List.of(new EarlyMarketIntradayBarSnapshot(
                        1L, date, stockCode, AT, LocalTime.of(9, 1),
                        BarInterval.ONE_MINUTE, amount(100), amount(102),
                        amount(99), amount(101), 10, amount(1000),
                        amount(100), "TEST"
                ));
            }

            @Override
            public List<EarlyMarketMarketSnapshot> loadMarketSnapshots(
                    LocalDate date,
                    String stockCode
            ) {
                return List.of(new EarlyMarketMarketSnapshot(
                        1L, date, stockCode, AT,
                        EarlyMarketSnapshotType.OPENING_0905, amount(101),
                        amount(102), amount(99), 10, amount(1000),
                        amount(100), "TEST"
                ));
            }
        };
    }

    private static BigDecimal amount(long value) {
        return BigDecimal.valueOf(value);
    }
}
