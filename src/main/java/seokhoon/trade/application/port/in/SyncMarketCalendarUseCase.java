package seokhoon.trade.application.port.in;

import java.time.LocalDate;

public interface SyncMarketCalendarUseCase {
    MarketCalendarSyncResult syncYear(int year);

    MarketCalendarSyncResult syncRange(LocalDate from, LocalDate to);
}
