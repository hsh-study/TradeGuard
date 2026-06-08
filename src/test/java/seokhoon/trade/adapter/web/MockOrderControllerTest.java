package seokhoon.trade.adapter.web;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.MockOrderResult;
import seokhoon.trade.application.port.in.OrderRequestView;
import seokhoon.trade.application.port.in.RequestStoredMockOrderUseCase;
import seokhoon.trade.domain.order.OrderRequest;
import seokhoon.trade.domain.order.OrderSide;
import seokhoon.trade.domain.order.OrderStatus;
import seokhoon.trade.domain.order.OrderType;
import seokhoon.trade.domain.risk.RiskDecision;
import seokhoon.trade.domain.strategy.SignalType;
import seokhoon.trade.domain.strategy.TradingSignal;
import seokhoon.trade.domain.strategy.TradingSignalStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MockOrderControllerTest {
    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 6, 5);

    @Test
    void returnsAcceptedMockOrderResult() {
        RequestStoredMockOrderUseCase requestUseCase = command -> {
            TradingSignal signal = signal();
            signal.approveRisk();
            signal.markOrderRequested();
            OrderRequest orderRequest = acceptedOrder();
            return MockOrderResult.accepted(RiskDecision.approve(), signal, orderRequest);
        };
        MockOrderController controller = new MockOrderController(
                requestUseCase,
                (stockCode, tradeDate, status, side) -> List.of()
        );

        MockOrderController.MockOrderResponse response = controller.request(request());

        assertThat(response.approved()).isTrue();
        assertThat(response.brokerFailed()).isFalse();
        assertThat(response.failureReason()).isNull();
        assertThat(response.rejectionReasons()).isEmpty();
        assertThat(response.signalStatus()).isEqualTo(TradingSignalStatus.ORDER_REQUESTED);
        assertThat(response.order().status()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(response.order().brokerOrderNo()).isEqualTo("FAKE-ORDER");
    }

    @Test
    void returnsBrokerFailureDetails() {
        Instant failedAt = Instant.parse("2026-06-05T06:01:00Z");
        RequestStoredMockOrderUseCase requestUseCase = command -> {
            TradingSignal signal = signal();
            signal.approveRisk();
            OrderRequest orderRequest = new OrderRequest(
                    "005930",
                    OrderSide.BUY,
                    OrderType.LIMIT,
                    1,
                    BigDecimal.valueOf(50_000),
                    "CLOSING_BET",
                    TRADE_DATE
            );
            orderRequest.markBrokerFailed("broker timeout", failedAt, true);
            return MockOrderResult.brokerFailed(RiskDecision.approve(), signal, orderRequest);
        };
        MockOrderController controller = new MockOrderController(
                requestUseCase,
                (stockCode, tradeDate, status, side) -> List.of()
        );

        MockOrderController.MockOrderResponse response = controller.request(request());

        assertThat(response.approved()).isFalse();
        assertThat(response.brokerFailed()).isTrue();
        assertThat(response.failureReason()).isEqualTo("broker timeout");
        assertThat(response.signalStatus()).isEqualTo(TradingSignalStatus.RISK_APPROVED);
        assertThat(response.order().status()).isEqualTo(OrderStatus.BROKER_FAILED);
        assertThat(response.order().brokerOrderNo()).isNull();
        assertThat(response.order().failedAt()).isEqualTo(failedAt);
        assertThat(response.order().retryable()).isTrue();
    }

    @Test
    void mapsOrderHistoryAndPassesFilters() {
        var filters = new Object[4];
        MockOrderController controller = new MockOrderController(
                command -> {
                    throw new UnsupportedOperationException();
                },
                (stockCode, tradeDate, status, side) -> {
                    filters[0] = stockCode;
                    filters[1] = tradeDate;
                    filters[2] = status;
                    filters[3] = side;
                    return List.of(acceptedOrderView());
                }
        );

        List<MockOrderController.OrderResponse> response = controller.find(
                "005930",
                TRADE_DATE,
                OrderStatus.ACCEPTED,
                OrderSide.BUY
        );

        assertThat(filters).containsExactly("005930", TRADE_DATE, OrderStatus.ACCEPTED, OrderSide.BUY);
        assertThat(response)
                .singleElement()
                .satisfies(order -> {
                    assertThat(order.orderId()).isEqualTo(10L);
                    assertThat(order.stockCode()).isEqualTo("005930");
                    assertThat(order.orderType()).isEqualTo(OrderType.LIMIT);
                    assertThat(order.status()).isEqualTo(OrderStatus.ACCEPTED);
                });
    }

    @Test
    void mapsBrokerFailureOrderHistory() {
        Instant failedAt = Instant.parse("2026-06-05T06:01:00Z");
        MockOrderController controller = new MockOrderController(
                command -> {
                    throw new UnsupportedOperationException();
                },
                (stockCode, tradeDate, status, side) -> {
                    assertThat(status).isEqualTo(OrderStatus.BROKER_FAILED);
                    return List.of(new OrderRequestView(
                            11L,
                            "005930",
                            OrderSide.BUY,
                            OrderType.LIMIT,
                            1,
                            BigDecimal.valueOf(50_000),
                            OrderStatus.BROKER_FAILED,
                            null,
                            "broker timeout",
                            failedAt,
                            true,
                            "CLOSING_BET",
                            TRADE_DATE
                    ));
                }
        );

        List<MockOrderController.OrderResponse> response = controller.find(
                null,
                null,
                OrderStatus.BROKER_FAILED,
                null
        );

        assertThat(response)
                .singleElement()
                .satisfies(order -> {
                    assertThat(order.orderId()).isEqualTo(11L);
                    assertThat(order.status()).isEqualTo(OrderStatus.BROKER_FAILED);
                    assertThat(order.brokerOrderNo()).isNull();
                    assertThat(order.failureReason()).isEqualTo("broker timeout");
                    assertThat(order.failedAt()).isEqualTo(failedAt);
                    assertThat(order.retryable()).isTrue();
                });
    }

    private static MockOrderController.MockOrderRequest request() {
        return new MockOrderController.MockOrderRequest(
                "CLOSING_BET",
                "005930",
                TRADE_DATE,
                SignalType.BUY_CANDIDATE,
                1,
                BigDecimal.valueOf(50_000)
        );
    }

    private static TradingSignal signal() {
        return new TradingSignal(
                "CLOSING_BET",
                "005930",
                TRADE_DATE,
                SignalType.BUY_CANDIDATE,
                80,
                List.of("TEST")
        );
    }

    private static OrderRequest acceptedOrder() {
        OrderRequest orderRequest = new OrderRequest(
                "005930",
                OrderSide.BUY,
                OrderType.LIMIT,
                1,
                BigDecimal.valueOf(50_000),
                "CLOSING_BET",
                TRADE_DATE
        );
        orderRequest.markRequested();
        orderRequest.accept("FAKE-ORDER");
        return orderRequest;
    }

    private static OrderRequestView acceptedOrderView() {
        return new OrderRequestView(
                10L,
                "005930",
                OrderSide.BUY,
                OrderType.LIMIT,
                1,
                BigDecimal.valueOf(50_000),
                OrderStatus.ACCEPTED,
                "FAKE-ORDER",
                null,
                null,
                false,
                "CLOSING_BET",
                TRADE_DATE
        );
    }
}
