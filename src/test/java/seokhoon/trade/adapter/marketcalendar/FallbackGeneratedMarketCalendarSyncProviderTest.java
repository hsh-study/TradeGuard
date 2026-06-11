package seokhoon.trade.adapter.marketcalendar;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FallbackGeneratedMarketCalendarSyncProviderTest {
    @Test
    void generatesFullYearWithWeekendManualLaborAndYearEndClosures() {
        KoreanMarketCalendarProperties properties =
                new KoreanMarketCalendarProperties();
        properties.setHolidays(List.of(LocalDate.of(2026, 6, 5)));
        FallbackGeneratedMarketCalendarSyncProvider provider =
                new FallbackGeneratedMarketCalendarSyncProvider(properties);

        var days = provider.fetchYear(2026);

        assertThat(days).hasSize(365);
        assertThat(find(days, LocalDate.of(2026, 5, 1)).tradingDay()).isFalse();
        assertThat(find(days, LocalDate.of(2026, 6, 5)).holidayName())
                .isEqualTo("MANUAL_OVERRIDE");
        assertThat(find(days, LocalDate.of(2026, 6, 6)).holidayName())
                .isEqualTo("WEEKEND");
        assertThat(find(days, LocalDate.of(2026, 12, 31)).holidayName())
                .isEqualTo("YEAR_END_MARKET_CLOSURE");
        assertThat(days).allMatch(day ->
                day.source().name().equals("FALLBACK_GENERATED"));
    }

    private static seokhoon.trade.domain.market.MarketCalendarDay find(
            List<seokhoon.trade.domain.market.MarketCalendarDay> days,
            LocalDate date
    ) {
        return days.stream()
                .filter(day -> day.date().equals(date))
                .findFirst()
                .orElseThrow();
    }
}
