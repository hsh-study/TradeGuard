package seokhoon.trade.application.port.in;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record MarketCalendarValidationResult(
        int year,
        int totalDays,
        int tradingDayCount,
        int holidayCount,
        int weekendTradingDayCount,
        int weekdayHolidayCount,
        List<LocalDate> weekendTradingDays,
        List<LocalDate> weekdayHolidays,
        List<LocalDate> missingDays,
        Map<String, Integer> sourceDistribution,
        List<String> warnings
) {
}
