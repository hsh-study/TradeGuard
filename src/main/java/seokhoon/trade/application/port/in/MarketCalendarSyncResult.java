package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.market.MarketCalendarSource;

import java.util.List;

public record MarketCalendarSyncResult(
        int syncedCount,
        int tradingDayCount,
        int holidayCount,
        MarketCalendarSource source,
        List<String> warnings
) {
}
