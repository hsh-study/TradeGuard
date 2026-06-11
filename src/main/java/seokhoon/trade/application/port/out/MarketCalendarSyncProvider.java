package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.market.MarketCalendarDay;

import java.util.List;

public interface MarketCalendarSyncProvider {
    List<MarketCalendarDay> fetchYear(int year);
}
