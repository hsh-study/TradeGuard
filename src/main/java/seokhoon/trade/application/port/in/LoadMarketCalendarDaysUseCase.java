package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.market.MarketCalendarDay;

import java.time.LocalDate;
import java.util.List;

public interface LoadMarketCalendarDaysUseCase {
    List<MarketCalendarDay> load(LocalDate from, LocalDate to);
}
