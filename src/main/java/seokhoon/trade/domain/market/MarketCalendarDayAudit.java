package seokhoon.trade.domain.market;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public record MarketCalendarDayAudit(
        Long id,
        String market,
        LocalDate date,
        Boolean beforeTradingDay,
        boolean afterTradingDay,
        String beforeHolidayName,
        String afterHolidayName,
        String reason,
        String actor,
        Instant createdAt
) {
    public MarketCalendarDayAudit {
        if (market == null || market.isBlank()) {
            market = MarketCalendarDay.KRX_STOCK;
        }
        Objects.requireNonNull(date, "date");
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason must not be blank");
        }
        if (actor == null || actor.isBlank()) {
            actor = "MANUAL_API";
        }
        Objects.requireNonNull(createdAt, "createdAt");
        reason = reason.trim();
        actor = actor.trim();
        beforeHolidayName = normalize(beforeHolidayName);
        afterHolidayName = normalize(afterHolidayName);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
