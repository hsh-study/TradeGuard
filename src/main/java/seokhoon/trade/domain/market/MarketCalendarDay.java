package seokhoon.trade.domain.market;

import java.time.LocalDate;
import java.util.Objects;

public record MarketCalendarDay(
        String market,
        LocalDate date,
        boolean tradingDay,
        String holidayName,
        MarketCalendarSource source
) {
    public static final String KRX_STOCK = "KRX_STOCK";

    public MarketCalendarDay {
        if (market == null || market.isBlank()) {
            market = KRX_STOCK;
        }
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(source, "source");
        holidayName = holidayName == null || holidayName.isBlank()
                ? null
                : holidayName.trim();
    }
}
