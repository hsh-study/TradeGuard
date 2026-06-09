package seokhoon.trade.adapter.web;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.MockOrderResult;
import seokhoon.trade.application.port.in.BrokerOrderRetryResult;
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
                    assertThat(order.signalId()).isEqualTo(1L);
                });
    }

    @Test
    void passesSignalIdOrderHistoryFilter() {
        var capturedSignalId = new Long[1];
        seokhoon.trade.application.port.in.LoadOrderRequestsUseCase loadUseCase =
                new seokhoon.trade.application.port.in.LoadOrderRequestsUseCase() {
                    @Override
                    public List<OrderRequestView> load(
                            String stockCode,
                            LocalDate tradeDate,
                            OrderStatus status,
                            OrderSide side
                    ) {
                        return List.of();
                    }

                    @Override
                    public List<OrderRequestView> load(
                            String stockCode,
                            LocalDate tradeDate,
                            OrderStatus status,
                            OrderSide side,
                            Long signalId
                    ) {
                        capturedSignalId[0] = signalId;
                        return List.of(acceptedOrderView());
                    }
                };
        MockOrderController controller = new MockOrderController(
                command -> {
                    throw new UnsupportedOperationException();
                },
                loadUseCase
        );

        List<MockOrderController.OrderResponse> response = controller.find(
                null,
                null,
                null,
                null,
                1L
        );

        assertThat(capturedSignalId[0]).isEqualTo(1L);
        assertThat(response).singleElement()
                .extracting(MockOrderController.OrderResponse::signalId)
                .isEqualTo(1L);
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
                            TRADE_DATE,
                            1L,
                            null
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

    @Test
    void retriesBrokerFailedOrderByOrderId() {
        OrderRequest accepted = new OrderRequest(
                "005930",
                OrderSide.BUY,
                OrderType.LIMIT,
                1,
                BigDecimal.valueOf(50_000),
                "CLOSING_BET",
                TRADE_DATE,
                21L
        );
        accepted.accept("FAKE-RETRY");
        MockOrderController controller = new MockOrderController(
                command -> {
                    throw new UnsupportedOperationException();
                },
                (stockCode, tradeDate, status, side) -> List.of(),
                orderId -> {
                    assertThat(orderId).isEqualTo(10L);
                    return BrokerOrderRetryResult.accepted(orderId, accepted);
                }
        );

        MockOrderController.RetryMockOrderResponse response = controller.retry(10L);

        assertThat(response.orderId()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(response.brokerFailed()).isFalse();
        assertThat(response.failureReason()).isNull();
        assertThat(response.failedAt()).isNull();
        assertThat(response.retryable()).isFalse();
        assertThat(response.brokerOrderNo()).isEqualTo("FAKE-RETRY");
        assertThat(response.signalId()).isEqualTo(21L);
    }

    @Test
    void returnsFailureDetailsWhenManualRetryFails() {
        Instant failedAt = Instant.parse("2026-06-05T06:10:00Z");
        OrderRequest failed = new OrderRequest(
                "005930",
                OrderSide.BUY,
                OrderType.LIMIT,
                1,
                BigDecimal.valueOf(50_000),
                "CLOSING_BET",
                TRADE_DATE
        );
        failed.markBrokerFailed("retry timeout", failedAt, true);
        MockOrderController controller = new MockOrderController(
                command -> {
                    throw new UnsupportedOperationException();
                },
                (stockCode, tradeDate, status, side) -> List.of(),
                orderId -> BrokerOrderRetryResult.brokerFailed(orderId, failed)
        );

        MockOrderController.RetryMockOrderResponse response = controller.retry(10L);

        assertThat(response.status()).isEqualTo(OrderStatus.BROKER_FAILED);
        assertThat(response.brokerFailed()).isTrue();
        assertThat(response.failureReason()).isEqualTo("retry timeout");
        assertThat(response.failedAt()).isEqualTo(failedAt);
        assertThat(response.retryable()).isTrue();
        assertThat(response.brokerOrderNo()).isNull();
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
                TRADE_DATE,
                1L,
                null
        );
    }
}
