package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.market.MarketCalendarDay;
import seokhoon.trade.domain.market.MarketCalendarDayAudit;

public record MarketCalendarDayOverrideResult(
        MarketCalendarDay day,
        MarketCalendarDayAudit audit
) {
}
