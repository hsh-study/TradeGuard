package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.order.LiveTradingReadinessReport;

public interface LiveTradingReadinessUseCase {
    LiveTradingReadinessReport checkReadiness();
}
