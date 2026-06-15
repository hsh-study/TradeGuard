package seokhoon.trade.adapter.web;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import seokhoon.trade.application.port.in.*;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.domain.indicator.*;
import seokhoon.trade.domain.stock.*;

import java.time.*;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class IndicatorWarmUpControllerTest {
    @Test
    void exposesWarmupActiveStocksHistoriesAndSnapshots()
            throws Exception {
        LocalDate baseDate = LocalDate.of(2026, 6, 15);
        IndicatorWarmUpResult result = new IndicatorWarmUpResult(
                "005930", baseDate, LocalDate.of(2025, 12, 24),
                LocalDate.of(2026, 6, 12), 120, 120, true,
                true, true, List.of(),
                IndicatorWarmUpStatus.SUCCEEDED);
        WarmUpDailyPricesAndIndicatorsUseCase warmUp =
                new WarmUpDailyPricesAndIndicatorsUseCase() {
                    @Override
                    public IndicatorWarmUpResult warmUpStock(
                            String stockCode, LocalDate date) {
                        return result;
                    }

                    @Override
                    public List<IndicatorWarmUpResult> warmUpStocks(
                            List<String> stockCodes, LocalDate date) {
                        return List.of(result);
                    }
                };
        FindStocksUseCase stocks = () -> List.of(
                new Stock("005930", "삼성전자", Market.KOSPI, true));
        IndicatorWarmUpHistoryPort histories =
                new IndicatorWarmUpHistoryPort() {
                    @Override
                    public IndicatorWarmUpHistory save(
                            IndicatorWarmUpResult value,
                            String failureReason, Instant createdAt) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public List<IndicatorWarmUpHistory> findByStockCode(
                            String stockCode) {
                        return List.of(new IndicatorWarmUpHistory(
                                1L, stockCode, baseDate,
                                IndicatorWarmUpStatus.SUCCEEDED,
                                120, 120, true, true, null,
                                Instant.parse("2026-06-15T00:00:00Z")));
                    }
                };
        IndicatorSnapshot snapshot = new IndicatorSnapshot(
                "005930", LocalDate.of(2026, 6, 12),
                decimal("70000"), decimal("69000"), decimal("68000"),
                decimal("55"), decimal("100"), decimal("90"),
                decimal("10"), decimal("72000"), decimal("69000"),
                decimal("66000"));
        IndicatorSnapshotPort snapshots = new IndicatorSnapshotPort() {
            @Override
            public IndicatorSnapshot save(IndicatorSnapshot value) {
                return value;
            }

            @Override
            public List<IndicatorSnapshot>
            findByStockCodeAndTradeDateBetween(
                    String stockCode, LocalDate from, LocalDate to) {
                return List.of(snapshot);
            }
        };
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new IndicatorWarmUpController(
                        warmUp, stocks, histories, snapshots,
                        Clock.fixed(
                                Instant.parse("2026-06-15T00:00:00Z"),
                                ZoneOffset.UTC))).build();

        mvc.perform(post("/api/indicators/warm-up")
                        .param("stockCode", "005930")
                        .param("baseDate", "2026-06-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.totalDailyPriceCount").value(120));
        mvc.perform(post("/api/indicators/warm-up/active-stocks")
                        .param("baseDate", "2026-06-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stockCode").value("005930"));
        mvc.perform(get("/api/indicators/warm-up/histories")
                        .param("stockCode", "005930"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("SUCCEEDED"));
        mvc.perform(get("/api/indicators/snapshots")
                        .param("stockCode", "005930")
                        .param("tradeDate", "2026-06-12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ma60").value(68000));
    }

    private static java.math.BigDecimal decimal(String value) {
        return new java.math.BigDecimal(value);
    }
}
