package seokhoon.trade.adapter.broker.kis;

import seokhoon.trade.application.port.out.BrokerPort;
import seokhoon.trade.domain.order.OrderRequest;

public class KisBrokerAdapter implements BrokerPort {
    @Override
    public OrderRequest requestOrder(OrderRequest orderRequest) {
        throw new UnsupportedOperationException("KIS live order API is intentionally not implemented in MVP");
    }
}
