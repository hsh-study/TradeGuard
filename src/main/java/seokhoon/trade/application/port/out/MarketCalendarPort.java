package seokhoon.trade.application.port.out;

import java.time.LocalDate;

public interface MarketCalendarPort {
    boolean isTradingDay(LocalDate date);

    default LocalDate previousTradingDay(LocalDate date) {
        return findTradingDay(date, -1);
    }

    default LocalDate nextTradingDay(LocalDate date) {
        return findTradingDay(date, 1);
    }

    private LocalDate findTradingDay(LocalDate date, int direction) {
        if (date == null) {
            throw new IllegalArgumentException("date must not be null");
        }
        LocalDate candidate = date.plusDays(direction);
        for (int days = 0; days < 370; days++) {
            if (isTradingDay(candidate)) {
                return candidate;
            }
            candidate = candidate.plusDays(direction);
        }
        throw new IllegalStateException("Trading day not found within 370 days");
    }
}
