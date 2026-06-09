package seokhoon.trade.application.port.in;

import java.util.List;

public interface LoadOrderStatusHistoriesUseCase {
    List<OrderStatusHistoryView> load(long orderId);
}
