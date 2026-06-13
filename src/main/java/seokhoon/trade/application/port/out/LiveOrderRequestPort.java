package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.order.LiveOrderRequest;
import seokhoon.trade.domain.order.LiveOrderStatus;
import seokhoon.trade.domain.order.OrderSide;

import java.util.List;
import java.util.Optional;

public interface LiveOrderRequestPort {
    LiveOrderRequest save(LiveOrderRequest order);
    Optional<LiveOrderRequest> findOrderById(long id);
    List<LiveOrderRequest> findAll();
    List<LiveOrderRequest> findByStatus(LiveOrderStatus status);
    List<LiveOrderRequest> findOpenSubmittedOrders();
    boolean existsBySignalIdAndSide(long signalId, OrderSide side);
}
