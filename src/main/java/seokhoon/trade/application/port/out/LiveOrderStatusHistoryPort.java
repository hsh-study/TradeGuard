package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.order.LiveOrderStatusHistory;

import java.util.List;

public interface LiveOrderStatusHistoryPort {
    void save(LiveOrderStatusHistory history);
    List<LiveOrderStatusHistory> findHistoriesByOrderId(long orderId);
}
