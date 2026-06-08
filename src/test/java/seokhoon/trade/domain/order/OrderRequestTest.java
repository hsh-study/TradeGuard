package seokhoon.trade.domain.order;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderRequestTest {
    @Test
    void marksBrokerFailureWithRetryMetadataAndClearsBrokerOrderNumber() {
        OrderRequest orderRequest = orderRequest();
        orderRequest.markRequested();
        Instant failedAt = Instant.parse("2026-06-05T06:01:00Z");

        orderRequest.markBrokerFailed("broker timeout", failedAt, true);

        assertThat(orderRequest.status()).isEqualTo(OrderStatus.BROKER_FAILED);
        assertThat(orderRequest.brokerOrderNo()).isNull();
        assertThat(orderRequest.failureReason()).isEqualTo("broker timeout");
        assertThat(orderRequest.failedAt()).isEqualTo(failedAt);
        assertThat(orderRequest.retryable()).isTrue();
    }

    @Test
    void doesNotOverwriteAcceptedOrderWithBrokerFailure() {
        OrderRequest orderRequest = orderRequest();
        orderRequest.accept("FAKE-ORDER");

        assertThatThrownBy(() -> orderRequest.markBrokerFailed(
                "late failure",
                Instant.parse("2026-06-05T06:01:00Z"),
                true
        ))
                .isInstanceOf(IllegalStateException.class);
        assertThat(orderRequest.status()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(orderRequest.brokerOrderNo()).isEqualTo("FAKE-ORDER");
    }

    private static OrderRequest orderRequest() {
        return new OrderRequest(
                "005930",
                OrderSide.BUY,
                OrderType.LIMIT,
                1,
                BigDecimal.valueOf(50_000),
                "CLOSING_BET",
                LocalDate.of(2026, 6, 5)
        );
    }
}
