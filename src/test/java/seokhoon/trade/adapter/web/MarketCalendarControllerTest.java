package seokhoon.trade.adapter.web;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import seokhoon.trade.application.port.in.MarketCalendarView;
import seokhoon.trade.application.port.in.MarketCalendarSyncResult;
import seokhoon.trade.application.port.in.SyncMarketCalendarUseCase;
import seokhoon.trade.domain.market.MarketCalendarDay;
import seokhoon.trade.domain.market.MarketCalendarSource;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MarketCalendarControllerTest {
    @Test
    void returnsTradingDayAndAdjacentTradingDays() {
        LocalDate holiday = LocalDate.of(2026, 6, 8);
        MarketCalendarController controller =
                new MarketCalendarController(
                        date -> new MarketCalendarView(
                                date,
                                false,
                                LocalDate.of(2026, 6, 5),
                                LocalDate.of(2026, 6, 9)
                        ),
                        (from, to) -> List.of(),
                        new NoopSyncUseCase()
                );

        var response = controller.find(holiday);

        assertThat(response.date()).isEqualTo(holiday);
        assertThat(response.tradingDay()).isFalse();
        assertThat(response.previousTradingDay())
                .isEqualTo(LocalDate.of(2026, 6, 5));
        assertThat(response.nextTradingDay())
                .isEqualTo(LocalDate.of(2026, 6, 9));
    }

    @Test
    void exposesSyncAndStoredCalendarDayApis() throws Exception {
        MarketCalendarController controller = new MarketCalendarController(
                date -> new MarketCalendarView(date, true, date.minusDays(1), date.plusDays(1)),
                (from, to) -> List.of(new MarketCalendarDay(
                        MarketCalendarDay.KRX_STOCK,
                        from,
                        false,
                        "HOLIDAY",
                        MarketCalendarSource.FALLBACK_GENERATED
                )),
                new SyncMarketCalendarUseCase() {
                    @Override
                    public MarketCalendarSyncResult syncYear(int year) {
                        return new MarketCalendarSyncResult(
                                365,
                                250,
                                115,
                                MarketCalendarSource.FALLBACK_GENERATED,
                                List.of("KRX_OFFICIAL_UNAVAILABLE")
                        );
                    }

                    @Override
                    public MarketCalendarSyncResult syncRange(
                            LocalDate from,
                            LocalDate to
                    ) {
                        throw new UnsupportedOperationException();
                    }
                }
        );
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(post("/api/market-calendar/sync").param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.syncedCount").value(365))
                .andExpect(jsonPath("$.source").value("FALLBACK_GENERATED"));
        mockMvc.perform(get("/api/market-calendar/days")
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].date").value("2026-01-01"))
                .andExpect(jsonPath("$[0].tradingDay").value(false));
    }

    private static class NoopSyncUseCase implements SyncMarketCalendarUseCase {
        @Override
        public MarketCalendarSyncResult syncYear(int year) {
            throw new UnsupportedOperationException();
        }

        @Override
        public MarketCalendarSyncResult syncRange(LocalDate from, LocalDate to) {
            throw new UnsupportedOperationException();
        }
    }
}
