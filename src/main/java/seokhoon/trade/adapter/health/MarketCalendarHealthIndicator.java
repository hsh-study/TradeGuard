package seokhoon.trade.adapter.health;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.out.MarketCalendarDayPort;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

@Component("marketCalendar")
public class MarketCalendarHealthIndicator implements HealthIndicator {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private final MarketCalendarDayPort calendarDayPort;
    private final Clock clock;

    @Autowired
    public MarketCalendarHealthIndicator(MarketCalendarDayPort calendarDayPort) {
        this(calendarDayPort, Clock.system(SEOUL));
    }

    MarketCalendarHealthIndicator(
            MarketCalendarDayPort calendarDayPort,
            Clock clock
    ) {
        this.calendarDayPort = calendarDayPort;
        this.clock = clock;
    }

    @Override
    public Health health() {
        LocalDate today = LocalDate.now(clock);
        boolean currentYearExists = calendarDayPort.existsByYear(today.getYear());
        if (!currentYearExists) {
            return Health.unknown()
                    .withDetail("currentYearCalendarExists", false)
                    .withDetail("fallbackAvailable", true)
                    .build();
        }
        boolean tradingDayWithin30Days = calendarDayPort.findBetween(
                        today,
                        today.plusDays(30)
                )
                .stream()
                .anyMatch(day -> day.tradingDay() && !day.date().isBefore(today));
        Health.Builder builder = tradingDayWithin30Days
                ? Health.up()
                : Health.down();
        return builder
                .withDetail("currentYearCalendarExists", true)
                .withDetail("tradingDayWithin30Days", tradingDayWithin30Days)
                .build();
    }
}
