package seokhoon.trade.adapter.broker;

import org.junit.jupiter.api.Test;
import seokhoon.trade.domain.order.OrderRequest;
import seokhoon.trade.domain.order.OrderSide;
import seokhoon.trade.domain.order.OrderStatus;
import seokhoon.trade.domain.order.OrderType;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class FakeBrokerAdapterTest {
    @Test
    void acceptsMockOrderRequest() {
        OrderRequest orderRequest = new OrderRequest("005930", OrderSide.BUY, OrderType.LIMIT, 1, BigDecimal.valueOf(50_000),
                "CLOSING_BET", LocalDate.of(2026, 2, 1));

        OrderRequest result = new FakeBrokerAdapter().requestOrder(orderRequest);

        assertThat(result.status()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(result.brokerOrderNo()).startsWith("FAKE-");
    }
}
