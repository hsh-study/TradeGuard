package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.out.MarketCalendarDayPort;
import seokhoon.trade.domain.market.MarketCalendarDay;
import seokhoon.trade.domain.market.MarketCalendarSource;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MarketCalendarSyncServiceTest {
    @Test
    void syncYearUpsertsOfficialDays() {
        InMemoryPort port = new InMemoryPort();
        MarketCalendarSyncService service = new MarketCalendarSyncService(
                port,
                year -> List.of(day(year, 1, true)),
                year -> List.of(day(year, 2, false)),
                seokhoon.trade.application.port.out.OperationalMetricsPort.noop(),
                true
        );

        var result = service.syncYear(2026);

        assertThat(result.syncedCount()).isEqualTo(1);
        assertThat(result.source()).isEqualTo(MarketCalendarSource.KRX_OFFICIAL);
        assertThat(port.days).hasSize(1);
    }

    @Test
    void fallsBackWhenOfficialProviderFails() {
        InMemoryPort port = new InMemoryPort();
        MarketCalendarSyncService service = new MarketCalendarSyncService(
                port,
                year -> {
                    throw new IllegalStateException("endpoint unavailable");
                },
                year -> List.of(day(year, 2, false)),
                seokhoon.trade.application.port.out.OperationalMetricsPort.noop(),
                true
        );

        var result = service.syncYear(2026);

        assertThat(result.source()).isEqualTo(MarketCalendarSource.FALLBACK_GENERATED);
        assertThat(result.warnings()).singleElement()
                .asString().contains("KRX_OFFICIAL_UNAVAILABLE");
        assertThat(port.days).hasSize(1);
    }

    private static MarketCalendarDay day(int year, int day, boolean tradingDay) {
        return new MarketCalendarDay(
                MarketCalendarDay.KRX_STOCK,
                LocalDate.ofYearDay(year, day),
                tradingDay,
                tradingDay ? null : "HOLIDAY",
                tradingDay
                        ? MarketCalendarSource.KRX_OFFICIAL
                        : MarketCalendarSource.FALLBACK_GENERATED
        );
    }

    private static class InMemoryPort implements MarketCalendarDayPort {
        private final List<MarketCalendarDay> days = new ArrayList<>();

        @Override
        public void upsertAll(List<MarketCalendarDay> newDays) {
            newDays.forEach(newDay -> {
                days.removeIf(day -> day.date().equals(newDay.date()));
                days.add(newDay);
            });
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
                    .sorted(Comparator.comparing(MarketCalendarDay::date))
                    .toList();
        }

        @Override
        public boolean existsByYear(int year) {
            return days.stream().anyMatch(day -> day.date().getYear() == year);
        }

        @Override
        public Optional<MarketCalendarDay> findPreviousTradingDay(LocalDate date) {
            return Optional.empty();
        }

        @Override
        public Optional<MarketCalendarDay> findNextTradingDay(LocalDate date) {
            return Optional.empty();
        }
    }
}
