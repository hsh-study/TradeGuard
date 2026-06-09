package seokhoon.trade.adapter.web;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.OrderRequestView;
import seokhoon.trade.config.OrderRetryProperties;
import seokhoon.trade.domain.order.OrderSide;
import seokhoon.trade.domain.order.OrderStatus;
import seokhoon.trade.domain.order.OrderType;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StuckRetryOrderControllerTest {
    private static final Instant NOW = Instant.parse("2026-06-05T06:10:00Z");

    @Test
    void loadsStuckRetriesWithRequestedThreshold() {
        var captured = new Object[2];
        OrderRetryProperties properties = new OrderRetryProperties();
        StuckRetryOrderController controller = new StuckRetryOrderController(
                (referenceTime, threshold) -> {
                    captured[0] = referenceTime;
                    captured[1] = threshold;
                    return List.of(view(OrderStatus.RETRY_REQUESTED, NOW.minusSeconds(360)));
                },
                (orderId, reason, referenceTime, threshold) -> {
                    throw new UnsupportedOperationException();
                },
                properties,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );

        List<StuckRetryOrderController.StuckRetryOrderResponse> result =
                controller.findStuckRetries(7L);

        assertThat(captured).containsExactly(NOW, Duration.ofMinutes(7));
        assertThat(result).singleElement().satisfies(order -> {
            assertThat(order.orderId()).isEqualTo(10L);
            assertThat(order.retryRequestedAt()).isEqualTo(NOW.minusSeconds(360));
        });
    }

    @Test
    void recoversWithConfiguredDefaultThreshold() {
        var captured = new Object[4];
        OrderRetryProperties properties = new OrderRetryProperties();
        properties.setRetryStuckThresholdMinutes(5);
        StuckRetryOrderController controller = new StuckRetryOrderController(
                (referenceTime, threshold) -> List.of(),
                (orderId, reason, referenceTime, threshold) -> {
                    captured[0] = orderId;
                    captured[1] = reason;
                    captured[2] = referenceTime;
                    captured[3] = threshold;
                    return view(OrderStatus.BROKER_FAILED, null);
                },
                properties,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );

        var result = controller.recover(
                10L,
                new StuckRetryOrderController.RecoverStuckRetryRequest(
                        "application restarted during retry"
                )
        );

        assertThat(captured).containsExactly(
                10L,
                "application restarted during retry",
                NOW,
                Duration.ofMinutes(5)
        );
        assertThat(result.status()).isEqualTo(OrderStatus.BROKER_FAILED);
    }

    private static OrderRequestView view(OrderStatus status, Instant retryRequestedAt) {
        return new OrderRequestView(
                10L,
                "005930",
                OrderSide.BUY,
                OrderType.LIMIT,
                1,
                BigDecimal.valueOf(50_000),
                status,
                null,
                status == OrderStatus.BROKER_FAILED ? "recovered" : null,
                status == OrderStatus.BROKER_FAILED ? NOW : null,
                true,
                "CLOSING_BET",
                LocalDate.of(2026, 6, 5),
                21L,
                retryRequestedAt
        );
    }
}
