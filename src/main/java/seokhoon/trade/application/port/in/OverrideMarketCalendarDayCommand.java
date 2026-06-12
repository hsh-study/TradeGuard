package seokhoon.trade.application.port.in;

import java.time.LocalDate;

public record OverrideMarketCalendarDayCommand(
        String market,
        LocalDate date,
        boolean tradingDay,
        String holidayName,
        String reason,
        String actor
) {
}
