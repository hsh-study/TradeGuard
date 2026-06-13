package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.order.LiveOrderRequest;
import seokhoon.trade.domain.order.LiveTradeFill;

import java.util.List;

public interface LiveTradingOrderPort {
    LiveOrderSubmission submitBuyLimitOrder(LiveOrderRequest order);
    LiveOrderSubmission submitSellLimitOrder(LiveOrderRequest order);
    LiveOrderSubmission inquireOrder(LiveOrderRequest order);
    List<LiveTradeFill> inquireFilledOrders(List<LiveOrderRequest> orders);
}
