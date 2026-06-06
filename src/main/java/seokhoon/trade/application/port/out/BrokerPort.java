package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.order.OrderRequest;

public interface BrokerPort {
    OrderRequest requestOrder(OrderRequest orderRequest);
}
