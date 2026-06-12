package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.market.MarketCalendarDayAudit;

import java.time.LocalDate;
import java.util.List;

public interface LoadMarketCalendarAuditsUseCase {
    List<MarketCalendarDayAudit> load(LocalDate from, LocalDate to);
}
