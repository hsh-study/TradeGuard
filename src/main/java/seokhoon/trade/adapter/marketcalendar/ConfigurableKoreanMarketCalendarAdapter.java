package seokhoon.trade.adapter.marketcalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.out.MarketCalendarDayPort;
import seokhoon.trade.application.port.out.MarketCalendarPort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.domain.market.MarketCalendarDay;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class ConfigurableKoreanMarketCalendarAdapter implements MarketCalendarPort {
    private static final Logger log = LoggerFactory.getLogger(
            ConfigurableKoreanMarketCalendarAdapter.class
    );
    private final Set<LocalDate> holidays;
    private final MarketCalendarDayPort calendarDayPort;
    private final OperationalMetricsPort metricsPort;

    @Autowired
    public ConfigurableKoreanMarketCalendarAdapter(
            KoreanMarketCalendarProperties properties,
            MarketCalendarDayPort calendarDayPort,
            OperationalMetricsPort metricsPort
    ) {
        this.holidays = Set.copyOf(properties.getHolidays());
        this.calendarDayPort = calendarDayPort;
        this.metricsPort = metricsPort;
    }

    ConfigurableKoreanMarketCalendarAdapter(
            KoreanMarketCalendarProperties properties
    ) {
        this(
                properties,
                new EmptyMarketCalendarDayPort(),
                OperationalMetricsPort.noop()
        );
    }

    @Override
    public boolean isTradingDay(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("date must not be null");
        }
        Optional<MarketCalendarDay> stored = calendarDayPort.findByDate(date);
        if (stored.isPresent()) {
            metricsPort.recordMarketCalendarLookup(
                    "db",
                    MarketCalendarDay.KRX_STOCK
            );
            return stored.get().tradingDay();
        }
        metricsPort.recordMarketCalendarLookup(
                "fallback",
                MarketCalendarDay.KRX_STOCK
        );
        return isFallbackTradingDay(date);
    }

    private boolean isFallbackTradingDay(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY
                && dayOfWeek != DayOfWeek.SUNDAY
                && !holidays.contains(date);
    }

    @Override
    public LocalDate previousTradingDay(LocalDate date) {
        validateDate(date);
        Optional<MarketCalendarDay> stored =
                calendarDayPort.findPreviousTradingDay(date);
        if (stored.isPresent()
                && hasCompleteRange(stored.get().date(), date.minusDays(1))) {
            metricsPort.recordMarketCalendarLookup(
                    "db",
                    MarketCalendarDay.KRX_STOCK
            );
            return stored.get().date();
        }
        warnIncompleteRange("previous", date);
        return findWithFallback(date, true);
    }

    @Override
    public LocalDate nextTradingDay(LocalDate date) {
        validateDate(date);
        Optional<MarketCalendarDay> stored =
                calendarDayPort.findNextTradingDay(date);
        if (stored.isPresent()
                && hasCompleteRange(date.plusDays(1), stored.get().date())) {
            metricsPort.recordMarketCalendarLookup(
                    "db",
                    MarketCalendarDay.KRX_STOCK
            );
            return stored.get().date();
        }
        warnIncompleteRange("next", date);
        return findWithFallback(date, false);
    }

    private LocalDate findWithFallback(LocalDate date, boolean previous) {
        try {
            return previous
                    ? MarketCalendarPort.super.previousTradingDay(date)
                    : MarketCalendarPort.super.nextTradingDay(date);
        } catch (IllegalStateException exception) {
            metricsPort.recordMarketCalendarLookup(
                    "not_found",
                    MarketCalendarDay.KRX_STOCK
            );
            throw exception;
        }
    }

    private boolean hasCompleteRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            return true;
        }
        long expected = ChronoUnit.DAYS.between(from, to) + 1;
        return calendarDayPort.findBetween(from, to).size() == expected;
    }

    private void warnIncompleteRange(String direction, LocalDate date) {
        log.atWarn()
                .addKeyValue("direction", direction)
                .addKeyValue("referenceDate", date)
                .addKeyValue("market", MarketCalendarDay.KRX_STOCK)
                .log("Market calendar DB range is incomplete; using fallback calculation");
    }

    private static void validateDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("date must not be null");
        }
    }

    private static final class EmptyMarketCalendarDayPort
            implements MarketCalendarDayPort {
        @Override
        public void upsertAll(List<MarketCalendarDay> days) {
        }

        @Override
        public Optional<MarketCalendarDay> findByDate(LocalDate date) {
            return Optional.empty();
        }

        @Override
        public List<MarketCalendarDay> findBetween(LocalDate from, LocalDate to) {
            return List.of();
        }

        @Override
        public boolean existsByYear(int year) {
            return false;
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
