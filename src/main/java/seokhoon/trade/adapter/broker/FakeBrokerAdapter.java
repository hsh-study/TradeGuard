package seokhoon.trade.adapter.broker;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.out.BrokerPort;
import seokhoon.trade.domain.order.OrderRequest;

import java.util.UUID;

@Primary
@Component
public class FakeBrokerAdapter implements BrokerPort {
    @Override
    public OrderRequest requestOrder(OrderRequest orderRequest) {
        orderRequest.markRequested();
        orderRequest.accept("FAKE-" + UUID.randomUUID());
        return orderRequest;
    }
}
