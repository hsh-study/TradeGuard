package seokhoon.trade.adapter.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import seokhoon.trade.application.port.in.LoadOrderStatusHistoriesUseCase;
import seokhoon.trade.application.port.in.LoadSignalStatusHistoriesUseCase;
import seokhoon.trade.application.port.in.OrderStatusHistoryView;
import seokhoon.trade.application.port.in.SignalStatusHistoryView;
import seokhoon.trade.domain.order.OrderStatus;
import seokhoon.trade.domain.strategy.TradingSignalStatus;

import java.time.Instant;
import java.util.List;

@RestController
public class StatusHistoryController {
    private final LoadSignalStatusHistoriesUseCase loadSignalHistories;
    private final LoadOrderStatusHistoriesUseCase loadOrderHistories;

    public StatusHistoryController(
            LoadSignalStatusHistoriesUseCase loadSignalHistories,
            LoadOrderStatusHistoriesUseCase loadOrderHistories
    ) {
        this.loadSignalHistories = loadSignalHistories;
        this.loadOrderHistories = loadOrderHistories;
    }

    @GetMapping("/api/signals/{signalId}/histories")
    List<SignalStatusHistoryResponse> signalHistories(@PathVariable long signalId) {
        return loadSignalHistories.load(signalId).stream()
                .map(SignalStatusHistoryResponse::from)
                .toList();
    }

    @GetMapping("/api/mock-orders/{orderId}/histories")
    List<OrderStatusHistoryResponse> orderHistories(@PathVariable long orderId) {
        return loadOrderHistories.load(orderId).stream()
                .map(OrderStatusHistoryResponse::from)
                .toList();
    }

    public record SignalStatusHistoryResponse(
            Long historyId,
            long signalId,
            TradingSignalStatus fromStatus,
            TradingSignalStatus toStatus,
            String reason,
            Instant createdAt
    ) {
        static SignalStatusHistoryResponse from(SignalStatusHistoryView view) {
            return new SignalStatusHistoryResponse(
                    view.historyId(),
                    view.signalId(),
                    view.fromStatus(),
                    view.toStatus(),
                    view.reason(),
                    view.createdAt()
            );
        }
    }

    public record OrderStatusHistoryResponse(
            Long historyId,
            long orderId,
            OrderStatus fromStatus,
            OrderStatus toStatus,
            String reason,
            Instant createdAt
    ) {
        static OrderStatusHistoryResponse from(OrderStatusHistoryView view) {
            return new OrderStatusHistoryResponse(
                    view.historyId(),
                    view.orderId(),
                    view.fromStatus(),
                    view.toStatus(),
                    view.reason(),
                    view.createdAt()
            );
        }
    }
}
