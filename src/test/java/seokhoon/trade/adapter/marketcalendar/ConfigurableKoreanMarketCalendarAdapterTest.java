package seokhoon.trade.adapter.marketcalendar;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigurableKoreanMarketCalendarAdapterTest {
    @Test
    void treatsWeekendAsNonTradingDay() {
        ConfigurableKoreanMarketCalendarAdapter calendar = calendar(List.of());

        assertThat(calendar.isTradingDay(LocalDate.of(2026, 6, 6))).isFalse();
        assertThat(calendar.isTradingDay(LocalDate.of(2026, 6, 7))).isFalse();
    }

    @Test
    void treatsConfiguredHolidayAsNonTradingDay() {
        LocalDate holiday = LocalDate.of(2026, 6, 5);
        ConfigurableKoreanMarketCalendarAdapter calendar = calendar(List.of(holiday));

        assertThat(calendar.isTradingDay(holiday)).isFalse();
    }

    @Test
    void treatsUnconfiguredWeekdayAsTradingDay() {
        ConfigurableKoreanMarketCalendarAdapter calendar = calendar(List.of());

        assertThat(calendar.isTradingDay(LocalDate.of(2026, 6, 5))).isTrue();
    }

    @Test
    void findsPreviousTradingDayAcrossWeekend() {
        ConfigurableKoreanMarketCalendarAdapter calendar = calendar(List.of());

        assertThat(calendar.previousTradingDay(LocalDate.of(2026, 6, 8)))
                .isEqualTo(LocalDate.of(2026, 6, 5));
    }

    @Test
    void skipsConfiguredHolidayWhenFindingPreviousTradingDay() {
        ConfigurableKoreanMarketCalendarAdapter calendar = calendar(List.of(
                LocalDate.of(2026, 6, 5)
        ));

        assertThat(calendar.previousTradingDay(LocalDate.of(2026, 6, 8)))
                .isEqualTo(LocalDate.of(2026, 6, 4));
    }

    @Test
    void findsPreviousTradingDayAcrossConfiguredHolidayPeriod() {
        ConfigurableKoreanMarketCalendarAdapter calendar = calendar(List.of(
                LocalDate.of(2026, 2, 16),
                LocalDate.of(2026, 2, 17),
                LocalDate.of(2026, 2, 18)
        ));

        assertThat(calendar.previousTradingDay(LocalDate.of(2026, 2, 19)))
                .isEqualTo(LocalDate.of(2026, 2, 13));
    }

    @Test
    void findsNextTradingDayAcrossWeekendAndHoliday() {
        ConfigurableKoreanMarketCalendarAdapter calendar = calendar(List.of(
                LocalDate.of(2026, 6, 8)
        ));

        assertThat(calendar.nextTradingDay(LocalDate.of(2026, 6, 5)))
                .isEqualTo(LocalDate.of(2026, 6, 9));
    }

    @Test
    void parsesConfiguredHolidayProperty() {
        new ApplicationContextRunner()
                .withUserConfiguration(CalendarPropertiesConfiguration.class)
                .withPropertyValues(
                        "tradeguard.market-calendar.holidays="
                                + "2026-01-01,2026-02-17,2026-02-18"
                )
                .run(context -> assertThat(context.getBean(
                        KoreanMarketCalendarProperties.class
                ).getHolidays()).containsExactly(
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 2, 17),
                        LocalDate.of(2026, 2, 18)
                ));
    }

    private static ConfigurableKoreanMarketCalendarAdapter calendar(List<LocalDate> holidays) {
        KoreanMarketCalendarProperties properties = new KoreanMarketCalendarProperties();
        properties.setHolidays(holidays);
        return new ConfigurableKoreanMarketCalendarAdapter(properties);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(KoreanMarketCalendarProperties.class)
    static class CalendarPropertiesConfiguration {
    }
}
