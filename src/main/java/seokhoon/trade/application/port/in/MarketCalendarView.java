package seokhoon.trade.application.port.in;

import java.time.LocalDate;

public record MarketCalendarView(
        LocalDate date,
        boolean tradingDay,
        LocalDate previousTradingDay,
        LocalDate nextTradingDay
) {
}
