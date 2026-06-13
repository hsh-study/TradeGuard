package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.order.LiveTradingRuntimeState;

public interface LiveTradingRuntimeStatePort {
    LiveTradingRuntimeState get();
    LiveTradingRuntimeState save(LiveTradingRuntimeState state);
}
