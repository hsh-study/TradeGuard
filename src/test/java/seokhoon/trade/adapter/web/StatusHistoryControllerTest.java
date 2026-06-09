package seokhoon.trade.adapter.web;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.OrderStatusHistoryView;
import seokhoon.trade.application.port.in.SignalStatusHistoryView;
import seokhoon.trade.domain.order.OrderStatus;
import seokhoon.trade.domain.strategy.TradingSignalStatus;
import seokhoon.trade.domain.audit.AuditActor;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StatusHistoryControllerTest {
    private static final Instant CREATED_AT = Instant.parse("2026-06-05T06:01:00Z");

    @Test
    void returnsSignalStatusHistories() {
        StatusHistoryController controller = new StatusHistoryController(
                signalId -> {
                    assertThat(signalId).isEqualTo(21L);
                    return List.of(new SignalStatusHistoryView(
                            1L,
                            signalId,
                            TradingSignalStatus.CREATED,
                            TradingSignalStatus.RISK_APPROVED,
                            "Risk policy approved",
                            AuditActor.API,
                            "request-123",
                            CREATED_AT
                    ));
                },
                orderId -> List.of()
        );

        var response = controller.signalHistories(21L);

        assertThat(response).singleElement().satisfies(history -> {
            assertThat(history.signalId()).isEqualTo(21L);
            assertThat(history.fromStatus()).isEqualTo(TradingSignalStatus.CREATED);
            assertThat(history.toStatus()).isEqualTo(TradingSignalStatus.RISK_APPROVED);
            assertThat(history.reason()).isEqualTo("Risk policy approved");
            assertThat(history.actor()).isEqualTo(AuditActor.API);
            assertThat(history.requestCorrelationId()).isEqualTo("request-123");
            assertThat(history.createdAt()).isEqualTo(CREATED_AT);
        });
    }

    @Test
    void returnsOrderStatusHistories() {
        StatusHistoryController controller = new StatusHistoryController(
                signalId -> List.of(),
                orderId -> {
                    assertThat(orderId).isEqualTo(10L);
                    return List.of(new OrderStatusHistoryView(
                            2L,
                            orderId,
                            OrderStatus.BROKER_FAILED,
                            OrderStatus.RETRY_REQUESTED,
                            "Manual broker retry requested",
                            AuditActor.API,
                            "request-456",
                            CREATED_AT
                    ));
                }
        );

        var response = controller.orderHistories(10L);

        assertThat(response).singleElement().satisfies(history -> {
            assertThat(history.orderId()).isEqualTo(10L);
            assertThat(history.fromStatus()).isEqualTo(OrderStatus.BROKER_FAILED);
            assertThat(history.toStatus()).isEqualTo(OrderStatus.RETRY_REQUESTED);
            assertThat(history.actor()).isEqualTo(AuditActor.API);
            assertThat(history.requestCorrelationId()).isEqualTo("request-456");
        });
    }
}
