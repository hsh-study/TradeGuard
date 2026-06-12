package seokhoon.trade.adapter.web;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import seokhoon.trade.application.port.in.MarketCalendarDayOverrideResult;
import seokhoon.trade.application.port.in.MarketCalendarSyncResult;
import seokhoon.trade.application.port.in.MarketCalendarValidationResult;
import seokhoon.trade.application.port.in.MarketCalendarView;
import seokhoon.trade.application.port.in.SyncMarketCalendarUseCase;
import seokhoon.trade.domain.market.MarketCalendarDay;
import seokhoon.trade.domain.market.MarketCalendarDayAudit;
import seokhoon.trade.domain.market.MarketCalendarSource;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
                        new NoopSyncUseCase(),
                        command -> {
                            throw new UnsupportedOperationException();
                        },
                        year -> {
                            throw new UnsupportedOperationException();
                        },
                        (from, to) -> List.of()
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
    void exposesCalendarManagementApis() throws Exception {
        MarketCalendarController controller = new MarketCalendarController(
                date -> new MarketCalendarView(date, true, date.minusDays(1), date.plusDays(1)),
                (from, to) -> List.of(new MarketCalendarDay(
                        MarketCalendarDay.KRX_STOCK,
                        from,
                        false,
                        "HOLIDAY",
                        MarketCalendarSource.FALLBACK_GENERATED
                )),
                syncUseCase(),
                command -> overrideResult(command.date()),
                MarketCalendarControllerTest::validationResult,
                (from, to) -> List.of(audit(from))
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
        mockMvc.perform(patch("/api/market-calendar/days/2026-08-17")
                        .contentType("application/json")
                        .content("""
                                {
                                  "tradingDay": false,
                                  "holidayName": "TEMPORARY_CLOSURE",
                                  "reason": "KRX correction"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.day.source").value("MANUAL_OVERRIDE"))
                .andExpect(jsonPath("$.audit.actor").value("MANUAL_API"));
        mockMvc.perform(get("/api/market-calendar/validation")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.sourceDistribution.MANUAL_OVERRIDE")
                        .value(1));
        mockMvc.perform(get("/api/market-calendar/audits")
                        .param("from", "2026-08-01")
                        .param("to", "2026-08-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reason").value("KRX correction"));
    }

    @Test
    void rejectsOverrideWithoutReason() throws Exception {
        MarketCalendarController controller = new MarketCalendarController(
                date -> new MarketCalendarView(date, true, date.minusDays(1), date.plusDays(1)),
                (from, to) -> List.of(),
                new NoopSyncUseCase(),
                command -> overrideResult(command.date()),
                MarketCalendarControllerTest::validationResult,
                (from, to) -> List.of()
        );
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(patch("/api/market-calendar/days/2026-08-17")
                        .contentType("application/json")
                        .content("""
                                {"tradingDay":false}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    private static SyncMarketCalendarUseCase syncUseCase() {
        return new SyncMarketCalendarUseCase() {
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
        };
    }

    private static MarketCalendarDayOverrideResult overrideResult(LocalDate date) {
        MarketCalendarDay day = new MarketCalendarDay(
                MarketCalendarDay.KRX_STOCK,
                date,
                false,
                "TEMPORARY_CLOSURE",
                MarketCalendarSource.MANUAL_OVERRIDE
        );
        return new MarketCalendarDayOverrideResult(day, audit(date));
    }

    private static MarketCalendarValidationResult validationResult(int year) {
        return new MarketCalendarValidationResult(
                year,
                365,
                250,
                115,
                0,
                115,
                List.of(),
                List.of(LocalDate.of(year, 1, 1)),
                List.of(),
                Map.of(
                        "MANUAL_OVERRIDE", 1,
                        "KRX_OFFICIAL", 364,
                        "FALLBACK_GENERATED", 0
                ),
                List.of()
        );
    }

    private static MarketCalendarDayAudit audit(LocalDate date) {
        return new MarketCalendarDayAudit(
                1L,
                MarketCalendarDay.KRX_STOCK,
                date,
                true,
                false,
                null,
                "TEMPORARY_CLOSURE",
                "KRX correction",
                "MANUAL_API",
                Instant.parse("2026-06-12T00:00:00Z")
        );
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
