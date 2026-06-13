package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.order.LiveTradeFill;

import java.util.List;

public interface LiveTradeFillPort {
    LiveTradeFill save(LiveTradeFill fill);
    List<LiveTradeFill> findFillsByOrderId(long orderId);
}
