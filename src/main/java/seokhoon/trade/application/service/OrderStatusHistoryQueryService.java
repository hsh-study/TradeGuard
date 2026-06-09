package seokhoon.trade.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.in.LoadOrderStatusHistoriesUseCase;
import seokhoon.trade.application.port.in.OrderStatusHistoryView;
import seokhoon.trade.application.port.out.OrderRequestPort;
import seokhoon.trade.application.port.out.OrderStatusHistoryPort;
import seokhoon.trade.application.port.out.OrderStatusHistoryRecord;

import java.util.List;

@Service
public class OrderStatusHistoryQueryService implements LoadOrderStatusHistoriesUseCase {
    private final OrderRequestPort orderRequestPort;
    private final OrderStatusHistoryPort historyPort;

    public OrderStatusHistoryQueryService(
            OrderRequestPort orderRequestPort,
            OrderStatusHistoryPort historyPort
    ) {
        this.orderRequestPort = orderRequestPort;
        this.historyPort = historyPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderStatusHistoryView> load(long orderId) {
        orderRequestPort.findById(orderId)
                .orElseThrow(() -> new OrderRequestNotFoundException(orderId));
        return historyPort.findByOrderRequestId(orderId).stream()
                .map(OrderStatusHistoryQueryService::toView)
                .toList();
    }

    private static OrderStatusHistoryView toView(OrderStatusHistoryRecord record) {
        return new OrderStatusHistoryView(
                record.id(),
                record.orderRequestId(),
                record.fromStatus(),
                record.toStatus(),
                record.reason(),
                record.createdAt()
        );
    }
}
