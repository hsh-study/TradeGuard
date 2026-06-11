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
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Qualifier("krxMarketCalendarSyncProvider")
public class KrxMarketCalendarSyncProvider implements MarketCalendarSyncProvider {
    private final KrxMarketCalendarClient client;
    private final KrxMarketCalendarParser parser;

    public KrxMarketCalendarSyncProvider(
            KrxMarketCalendarClient client,
            KrxMarketCalendarParser parser
    ) {
        this.client = client;
        this.parser = parser;
    }

    @Override
    public List<MarketCalendarDay> fetchYear(int year) {
        List<MarketCalendarDay> parsed = parser.parse(client.fetchYear(year), year);
        int expectedDays = LocalDate.of(year, 1, 1).lengthOfYear();
        if (parsed.size() == expectedDays) {
            return parsed;
        }
        Map<LocalDate, MarketCalendarDay> officialByDate = parsed.stream()
                .collect(Collectors.toMap(
                        MarketCalendarDay::date,
                        Function.identity(),
                        (left, right) -> right
                ));
        List<MarketCalendarDay> normalized = new ArrayList<>();
        LocalDate date = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        while (!date.isAfter(end)) {
            MarketCalendarDay official = officialByDate.get(date);
            if (official != null) {
                normalized.add(official);
            } else {
                boolean weekend = date.getDayOfWeek() == DayOfWeek.SATURDAY
                        || date.getDayOfWeek() == DayOfWeek.SUNDAY;
                normalized.add(new MarketCalendarDay(
                        MarketCalendarDay.KRX_STOCK,
                        date,
                        !weekend,
                        weekend ? "WEEKEND" : null,
                        MarketCalendarSource.KRX_OFFICIAL
                ));
            }
            date = date.plusDays(1);
        }
        return List.copyOf(normalized);
    }
}
