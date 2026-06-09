package seokhoon.trade.adapter.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import seokhoon.trade.application.port.out.DuplicateOrderRequestException;
import seokhoon.trade.application.port.out.OrderRequestPort;
import seokhoon.trade.application.port.in.RetryBrokerFailedOrderUseCase;
import seokhoon.trade.domain.order.OrderRequest;
import seokhoon.trade.domain.order.OrderSide;
import seokhoon.trade.domain.order.OrderStatus;
import seokhoon.trade.domain.order.OrderType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class OrderRequestDuplicateIntegrationTest {
    @Autowired
    private OrderRequestPort orderRequestPort;

    @Autowired
    private OrderRequestJpaRepository repository;

    @Autowired
    private RetryBrokerFailedOrderUseCase retryBrokerFailedOrderUseCase;

    @BeforeEach
    void clearOrders() {
        repository.deleteAll();
    }

    @Test
    void rejectsDuplicateLogicalOrderAndKeepsSingleReservation() {
        orderRequestPort.create(orderRequest());

        assertThatThrownBy(() -> orderRequestPort.create(orderRequest()))
                .isInstanceOf(DuplicateOrderRequestException.class);
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void updatesExistingReservationWithoutCreatingAnotherRow() {
        OrderRequest orderRequest = orderRequest();
        orderRequestPort.create(orderRequest);
        orderRequest.markRequested();
        orderRequest.accept("FAKE-ORDER");

        orderRequestPort.update(orderRequest);

        assertThat(repository.findAll())
                .singleElement()
                .extracting(OrderRequestEntity::status)
                .isEqualTo(OrderStatus.ACCEPTED);
    }

    @Test
    void filtersOrderHistoryByStockDateAndStatus() {
        OrderRequest accepted = orderRequest();
        orderRequestPort.create(accepted);
        accepted.markRequested();
        accepted.accept("FAKE-ORDER");
        orderRequestPort.update(accepted);
        orderRequestPort.create(new OrderRequest(
                "000660",
                OrderSide.BUY,
                OrderType.LIMIT,
                1,
                BigDecimal.valueOf(90_000),
                "CLOSING_BET",
                LocalDate.of(2026, 6, 6)
        ));

        assertThat(orderRequestPort.find("005930", LocalDate.of(2026, 6, 5), OrderStatus.ACCEPTED, OrderSide.BUY))
                .singleElement()
                .satisfies(order -> {
                    assertThat(order.stockCode()).isEqualTo("005930");
                    assertThat(order.status()).isEqualTo(OrderStatus.ACCEPTED);
                    assertThat(order.brokerOrderNo()).isEqualTo("FAKE-ORDER");
                });
    }

    @Test
    void persistsAndLoadsBrokerFailureDetails() {
        Instant failedAt = Instant.parse("2026-06-05T06:01:00Z");
        OrderRequest failed = orderRequest();
        orderRequestPort.create(failed);
        failed.markBrokerFailed("broker timeout", failedAt, true);

        orderRequestPort.update(failed);

        assertThat(orderRequestPort.find(
                "005930",
                LocalDate.of(2026, 6, 5),
                OrderStatus.BROKER_FAILED,
                OrderSide.BUY
        ))
                .singleElement()
                .satisfies(order -> {
                    assertThat(order.status()).isEqualTo(OrderStatus.BROKER_FAILED);
                    assertThat(order.brokerOrderNo()).isNull();
                    assertThat(order.failureReason()).isEqualTo("broker timeout");
                    assertThat(order.failedAt()).isEqualTo(failedAt);
                    assertThat(order.retryable()).isTrue();
                });
    }

    @Test
    void claimsRetryByUpdatingExistingRowWithoutCreatingNewOrder() {
        OrderRequest failed = orderRequest();
        orderRequestPort.create(failed);
        failed.markBrokerFailed(
                "broker timeout",
                Instant.parse("2026-06-05T06:01:00Z"),
                true
        );
        orderRequestPort.update(failed);
        long orderId = repository.findAll().getFirst().toRecord().id();

        boolean firstClaim = orderRequestPort.claimRetry(orderId);
        boolean secondClaim = orderRequestPort.claimRetry(orderId);

        assertThat(firstClaim).isTrue();
        assertThat(secondClaim).isFalse();
        assertThat(repository.count()).isEqualTo(1);
        assertThat(orderRequestPort.findById(orderId))
                .hasValueSatisfying(order ->
                        assertThat(order.status()).isEqualTo(OrderStatus.RETRY_REQUESTED));
    }

    @Test
    void retriesFailedOrderWithoutCreatingNewRow() {
        OrderRequest failed = orderRequest();
        orderRequestPort.create(failed);
        failed.markBrokerFailed(
                "broker timeout",
                Instant.parse("2026-06-05T06:01:00Z"),
                true
        );
        orderRequestPort.update(failed);
        long orderId = repository.findAll().getFirst().toRecord().id();

        var result = retryBrokerFailedOrderUseCase.retry(orderId);

        assertThat(repository.count()).isEqualTo(1);
        assertThat(result.orderId()).isEqualTo(orderId);
        assertThat(result.orderRequest().status()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(result.orderRequest().brokerOrderNo()).startsWith("FAKE-");
        assertThat(orderRequestPort.findById(orderId))
                .hasValueSatisfying(order -> {
                    assertThat(order.status()).isEqualTo(OrderStatus.ACCEPTED);
                    assertThat(order.brokerOrderNo()).startsWith("FAKE-");
                });
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
