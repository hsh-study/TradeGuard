package seokhoon.trade.application.port.in;

import java.time.LocalDate;

public interface LoadMarketCalendarUseCase {
    MarketCalendarView load(LocalDate date);
}
