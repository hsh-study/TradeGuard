package seokhoon.trade.application.port.out;

import java.time.LocalDate;

public interface MarketCalendarPort {
    boolean isTradingDay(LocalDate date);
}
