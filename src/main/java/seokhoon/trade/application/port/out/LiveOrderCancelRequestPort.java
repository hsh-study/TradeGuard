package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.order.LiveOrderCancelRequest;

import java.util.List;

public interface LiveOrderCancelRequestPort {
    LiveOrderCancelRequest save(LiveOrderCancelRequest request);
    List<LiveOrderCancelRequest> findByOrderId(long orderId);
}
