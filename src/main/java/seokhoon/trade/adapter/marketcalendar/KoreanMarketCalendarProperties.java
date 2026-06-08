package seokhoon.trade.adapter.marketcalendar;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "tradeguard.market-calendar")
public class KoreanMarketCalendarProperties {
    private List<LocalDate> holidays = new ArrayList<>();

    public List<LocalDate> getHolidays() {
        return holidays;
    }

    public void setHolidays(List<LocalDate> holidays) {
        this.holidays = holidays == null ? new ArrayList<>() : new ArrayList<>(holidays);
    }
}
