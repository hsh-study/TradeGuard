package seokhoon.trade.adapter.marketcalendar;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import seokhoon.trade.application.port.out.MarketCalendarDayPort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.domain.market.MarketCalendarDay;
import seokhoon.trade.domain.market.MarketCalendarSource;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
    void usesStoredHolidayAndTradingDayBeforeFallback() {
        InMemoryCalendarDayPort port = new InMemoryCalendarDayPort(List.of(
                day(LocalDate.of(2026, 6, 5), false),
                day(LocalDate.of(2026, 6, 6), true)
        ));
        ConfigurableKoreanMarketCalendarAdapter calendar =
                calendar(List.of(), port);

        assertThat(calendar.isTradingDay(LocalDate.of(2026, 6, 5))).isFalse();
        assertThat(calendar.isTradingDay(LocalDate.of(2026, 6, 6))).isTrue();
    }

    @Test
    void manualOverrideTakesPriorityOverRuntimeWeekendFallback() {
        LocalDate saturday = LocalDate.of(2026, 6, 6);
        InMemoryCalendarDayPort port = new InMemoryCalendarDayPort(List.of(
                new MarketCalendarDay(
                        MarketCalendarDay.KRX_STOCK,
                        saturday,
                        true,
                        null,
                        MarketCalendarSource.MANUAL_OVERRIDE
                )
        ));
        ConfigurableKoreanMarketCalendarAdapter calendar =
                calendar(List.of(), port);

        assertThat(calendar.isTradingDay(saturday)).isTrue();
    }

    @Test
    void usesCompleteStoredRangeForPreviousAndNextTradingDay() {
        InMemoryCalendarDayPort port = new InMemoryCalendarDayPort(List.of(
                day(LocalDate.of(2026, 6, 5), true),
                day(LocalDate.of(2026, 6, 6), false),
                day(LocalDate.of(2026, 6, 7), false),
                day(LocalDate.of(2026, 6, 8), false),
                day(LocalDate.of(2026, 6, 9), true)
        ));
        ConfigurableKoreanMarketCalendarAdapter calendar =
                calendar(List.of(), port);

        assertThat(calendar.previousTradingDay(LocalDate.of(2026, 6, 9)))
                .isEqualTo(LocalDate.of(2026, 6, 5));
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

    private static ConfigurableKoreanMarketCalendarAdapter calendar(
            List<LocalDate> holidays,
            MarketCalendarDayPort port
    ) {
        KoreanMarketCalendarProperties properties = new KoreanMarketCalendarProperties();
        properties.setHolidays(holidays);
        return new ConfigurableKoreanMarketCalendarAdapter(
                properties,
                port,
                OperationalMetricsPort.noop()
        );
    }

    private static MarketCalendarDay day(LocalDate date, boolean tradingDay) {
        return new MarketCalendarDay(
                MarketCalendarDay.KRX_STOCK,
                date,
                tradingDay,
                tradingDay ? null : "HOLIDAY",
                MarketCalendarSource.KRX_OFFICIAL
        );
    }

    private static class InMemoryCalendarDayPort implements MarketCalendarDayPort {
        private final List<MarketCalendarDay> days;

        private InMemoryCalendarDayPort(List<MarketCalendarDay> days) {
            this.days = days;
        }

        @Override
        public void upsertAll(List<MarketCalendarDay> days) {
            throw new UnsupportedOperationException();
        }

        @Override
        public MarketCalendarDay save(MarketCalendarDay day) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<MarketCalendarDay> findByDate(LocalDate date) {
            return days.stream().filter(day -> day.date().equals(date)).findFirst();
        }

        @Override
        public List<MarketCalendarDay> findBetween(LocalDate from, LocalDate to) {
            return days.stream()
                    .filter(day -> !day.date().isBefore(from))
                    .filter(day -> !day.date().isAfter(to))
                    .toList();
        }

        @Override
        public boolean existsByYear(int year) {
            return days.stream().anyMatch(day -> day.date().getYear() == year);
        }

        @Override
        public Optional<MarketCalendarDay> findPreviousTradingDay(LocalDate date) {
            return days.stream()
                    .filter(MarketCalendarDay::tradingDay)
                    .filter(day -> day.date().isBefore(date))
                    .max(java.util.Comparator.comparing(MarketCalendarDay::date));
        }

        @Override
        public Optional<MarketCalendarDay> findNextTradingDay(LocalDate date) {
            return days.stream()
                    .filter(MarketCalendarDay::tradingDay)
                    .filter(day -> day.date().isAfter(date))
                    .min(java.util.Comparator.comparing(MarketCalendarDay::date));
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(KoreanMarketCalendarProperties.class)
    static class CalendarPropertiesConfiguration {
    }
}
