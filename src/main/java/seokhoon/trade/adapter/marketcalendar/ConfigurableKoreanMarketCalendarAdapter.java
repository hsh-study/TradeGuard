package seokhoon.trade.adapter.marketcalendar;

import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.out.MarketCalendarPort;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

@Component
public class ConfigurableKoreanMarketCalendarAdapter implements MarketCalendarPort {
    private final Set<LocalDate> holidays;

    public ConfigurableKoreanMarketCalendarAdapter(KoreanMarketCalendarProperties properties) {
        this.holidays = Set.copyOf(properties.getHolidays());
    }

    @Override
    public boolean isTradingDay(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY
                && dayOfWeek != DayOfWeek.SUNDAY
                && !holidays.contains(date);
    }
}
