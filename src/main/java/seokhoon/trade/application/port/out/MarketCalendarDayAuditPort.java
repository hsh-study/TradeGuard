package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.market.MarketCalendarDayAudit;

import java.time.LocalDate;
import java.util.List;

public interface MarketCalendarDayAuditPort {
    MarketCalendarDayAudit save(MarketCalendarDayAudit audit);

    List<MarketCalendarDayAudit> findBetween(LocalDate from, LocalDate to);
}
