package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.out.MarketCalendarPort;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class MarketCalendarServiceTest {
    @Test
    void loadsTradingDayContextFromPort() {
        LocalDate holiday = LocalDate.of(2026, 6, 8);
        MarketCalendarPort calendar = date ->
                date.getDayOfWeek() != DayOfWeek.SATURDAY
                        && date.getDayOfWeek() != DayOfWeek.SUNDAY
                        && !date.equals(holiday);
        MarketCalendarService service = new MarketCalendarService(calendar);

        var view = service.load(holiday);

        assertThat(view.tradingDay()).isFalse();
        assertThat(view.previousTradingDay())
                .isEqualTo(LocalDate.of(2026, 6, 5));
        assertThat(view.nextTradingDay())
                .isEqualTo(LocalDate.of(2026, 6, 9));
    }
}
