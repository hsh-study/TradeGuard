package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.market.MarketCalendarDay;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MarketCalendarDayPort {
    void upsertAll(List<MarketCalendarDay> days);

    Optional<MarketCalendarDay> findByDate(LocalDate date);

    List<MarketCalendarDay> findBetween(LocalDate from, LocalDate to);

    boolean existsByYear(int year);

    Optional<MarketCalendarDay> findPreviousTradingDay(LocalDate date);

    Optional<MarketCalendarDay> findNextTradingDay(LocalDate date);
}
