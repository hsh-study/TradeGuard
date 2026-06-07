package seokhoon.trade.adapter.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import seokhoon.trade.application.port.out.DuplicateOrderRequestException;
import seokhoon.trade.application.port.out.OrderRequestPort;
import seokhoon.trade.domain.order.OrderRequest;
import seokhoon.trade.domain.order.OrderSide;
import seokhoon.trade.domain.order.OrderStatus;
import seokhoon.trade.domain.order.OrderType;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class OrderRequestDuplicateIntegrationTest {
    @Autowired
    private OrderRequestPort orderRequestPort;

    @Autowired
    private OrderRequestJpaRepository repository;

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
