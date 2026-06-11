package seokhoon.trade.adapter.web;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.MarketCalendarView;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class MarketCalendarControllerTest {
    @Test
    void returnsTradingDayAndAdjacentTradingDays() {
        LocalDate holiday = LocalDate.of(2026, 6, 8);
        MarketCalendarController controller =
                new MarketCalendarController(date -> new MarketCalendarView(
                        date,
                        false,
                        LocalDate.of(2026, 6, 5),
                        LocalDate.of(2026, 6, 9)
                ));

        var response = controller.find(holiday);

        assertThat(response.date()).isEqualTo(holiday);
        assertThat(response.tradingDay()).isFalse();
        assertThat(response.previousTradingDay())
                .isEqualTo(LocalDate.of(2026, 6, 5));
        assertThat(response.nextTradingDay())
                .isEqualTo(LocalDate.of(2026, 6, 9));
    }
}
