package seokhoon.trade.adapter.health;

import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Status;
import seokhoon.trade.application.port.out.MarketCalendarDayPort;
import seokhoon.trade.domain.market.MarketCalendarDay;
import seokhoon.trade.domain.market.MarketCalendarSource;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MarketCalendarHealthIndicatorTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-11T03:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @Test
    void reportsUnknownWhenCurrentYearCalendarIsMissing() {
        MarketCalendarDayPort port = mock(MarketCalendarDayPort.class);
        when(port.existsByYear(2026)).thenReturn(false);

        var health = new MarketCalendarHealthIndicator(port, CLOCK).health();

        assertThat(health.getStatus()).isEqualTo(Status.UNKNOWN);
    }

    @Test
    void reportsUpWhenStoredCalendarHasUpcomingTradingDay() {
        MarketCalendarDayPort port = mock(MarketCalendarDayPort.class);
        when(port.existsByYear(2026)).thenReturn(true);
        when(port.findBetween(
                LocalDate.of(2026, 6, 11),
                LocalDate.of(2026, 7, 11)
        )).thenReturn(List.of(new MarketCalendarDay(
                MarketCalendarDay.KRX_STOCK,
                LocalDate.of(2026, 6, 12),
                true,
                null,
                MarketCalendarSource.KRX_OFFICIAL
        )));

        var health = new MarketCalendarHealthIndicator(port, CLOCK).health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
    }
}
