package seokhoon.trade.adapter.marketcalendar;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.out.MarketCalendarSyncProvider;
import seokhoon.trade.domain.market.MarketCalendarDay;
import seokhoon.trade.domain.market.MarketCalendarSource;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Qualifier("fallbackGeneratedMarketCalendarSyncProvider")
public class FallbackGeneratedMarketCalendarSyncProvider
        implements MarketCalendarSyncProvider {
    private final Set<LocalDate> configuredHolidays;

    public FallbackGeneratedMarketCalendarSyncProvider(
            KoreanMarketCalendarProperties properties
    ) {
        this.configuredHolidays = Set.copyOf(properties.getHolidays());
    }

    @Override
    public List<MarketCalendarDay> fetchYear(int year) {
        LocalDate yearEndClosure = findYearEndClosure(year);
        Map<LocalDate, String> namedClosures = configuredHolidays.stream()
                .filter(date -> date.getYear() == year)
                .collect(Collectors.toMap(
                        Function.identity(),
                        ignored -> "MANUAL_OVERRIDE"
                ));
        namedClosures.put(LocalDate.of(year, 5, 1), "LABOR_DAY");
        namedClosures.put(yearEndClosure, "YEAR_END_MARKET_CLOSURE");

        List<MarketCalendarDay> days = new ArrayList<>();
        LocalDate date = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        while (!date.isAfter(end)) {
            String holidayName = namedClosures.get(date);
            if (isWeekend(date)) {
                holidayName = "WEEKEND";
            }
            days.add(new MarketCalendarDay(
                    MarketCalendarDay.KRX_STOCK,
                    date,
                    holidayName == null,
                    holidayName,
                    MarketCalendarSource.FALLBACK_GENERATED
            ));
            date = date.plusDays(1);
        }
        return List.copyOf(days);
    }

    private LocalDate findYearEndClosure(int year) {
        LocalDate candidate = LocalDate.of(year, 12, 31);
        while (isWeekend(candidate) || configuredHolidays.contains(candidate)) {
            candidate = candidate.minusDays(1);
        }
        return candidate;
    }

    private static boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY
                || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }
}
