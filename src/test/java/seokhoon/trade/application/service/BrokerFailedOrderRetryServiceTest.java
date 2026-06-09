package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.BrokerOrderRetryResult;
import seokhoon.trade.application.port.out.BrokerPort;
import seokhoon.trade.application.port.out.OrderRequestPort;
import seokhoon.trade.application.port.out.OrderRequestRecord;
import seokhoon.trade.application.port.out.OrderStatusHistoryPort;
import seokhoon.trade.application.port.out.OrderStatusHistoryRecord;
import seokhoon.trade.application.port.out.SignalStatusHistoryPort;
import seokhoon.trade.application.port.out.TradingSignalPort;
import seokhoon.trade.domain.order.OrderRequest;
import seokhoon.trade.domain.order.OrderSide;
import seokhoon.trade.domain.order.OrderStatus;
import seokhoon.trade.domain.order.OrderType;
import seokhoon.trade.domain.strategy.SignalType;
import seokhoon.trade.domain.strategy.TradingSignal;
import seokhoon.trade.domain.strategy.TradingSignalStatus;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BrokerFailedOrderRetryServiceTest {
    private static final long ORDER_ID = 10L;

    @Test
    void retriesBrokerFailedOrderAndUpdatesSameOrderAsAccepted() {
        RecordingOrderPort orderPort = new RecordingOrderPort(failedOrder(true));
        RecordingBrokerPort brokerPort = new RecordingBrokerPort(false);
        RecordingOrderHistoryPort historyPort = new RecordingOrderHistoryPort();
        BrokerFailedOrderRetryService service = service(orderPort, brokerPort, historyPort);

        BrokerOrderRetryResult result = service.retry(ORDER_ID);

        assertThat(result.orderId()).isEqualTo(ORDER_ID);
        assertThat(result.brokerFailed()).isFalse();
        assertThat(result.orderRequest().status()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(result.orderRequest().brokerOrderNo()).isEqualTo("FAKE-RETRY");
        assertThat(result.orderRequest().failureReason()).isNull();
        assertThat(result.orderRequest().failedAt()).isNull();
        assertThat(result.orderRequest().retryable()).isFalse();
        assertThat(orderPort.createCalls).isZero();
        assertThat(orderPort.updateByIdCalls).isEqualTo(1);
        assertThat(orderPort.updatedOrderId).isEqualTo(ORDER_ID);
        assertThat(brokerPort.calls).isEqualTo(1);
        assertThat(historyPort.records)
                .extracting(
                        OrderStatusHistoryRecord::fromStatus,
                        OrderStatusHistoryRecord::toStatus
                )
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(
                                OrderStatus.BROKER_FAILED,
                                OrderStatus.RETRY_REQUESTED
                        ),
                        org.assertj.core.groups.Tuple.tuple(
                                OrderStatus.RETRY_REQUESTED,
                                OrderStatus.ACCEPTED
                        )
                );
    }

    @Test
    void marksLinkedTradingSignalOrderRequestedAfterSuccessfulRetry() {
        TradingSignal signal = signal();
        RecordingSignalPort signalPort = new RecordingSignalPort(signal);
        RecordingOrderPort orderPort = new RecordingOrderPort(failedOrder(true, 21L));
        BrokerFailedOrderRetryService service = new BrokerFailedOrderRetryService(
                orderPort,
                new RecordingBrokerPort(false),
                signalPort,
                Clock.fixed(Instant.parse("2026-06-05T06:10:00Z"), ZoneOffset.UTC)
        );

        service.retry(ORDER_ID);

        assertThat(signalPort.requestedSignalId).isEqualTo(21L);
        assertThat(signalPort.savedSignal.status())
                .isEqualTo(seokhoon.trade.domain.strategy.TradingSignalStatus.ORDER_REQUESTED);
    }

    @Test
    void keepsBrokerFailedAndRefreshesFailureDetailsWhenRetryFails() {
        RecordingOrderPort orderPort = new RecordingOrderPort(failedOrder(true, 21L));
        RecordingBrokerPort brokerPort = new RecordingBrokerPort(true);
        RecordingSignalPort signalPort = new RecordingSignalPort(signal());
        RecordingOrderHistoryPort historyPort = new RecordingOrderHistoryPort();
        BrokerFailedOrderRetryService service = new BrokerFailedOrderRetryService(
                orderPort,
                brokerPort,
                signalPort,
                historyPort,
                SignalStatusHistoryPort.noop(),
                Clock.fixed(Instant.parse("2026-06-05T06:10:00Z"), ZoneOffset.UTC)
        );

        BrokerOrderRetryResult result = service.retry(ORDER_ID);

        assertThat(result.brokerFailed()).isTrue();
        assertThat(result.orderRequest().status()).isEqualTo(OrderStatus.BROKER_FAILED);
        assertThat(result.orderRequest().brokerOrderNo()).isNull();
        assertThat(result.orderRequest().failureReason()).isEqualTo("retry timeout");
        assertThat(result.orderRequest().failedAt())
                .isEqualTo(Instant.parse("2026-06-05T06:10:00Z"));
        assertThat(result.orderRequest().retryable()).isTrue();
        assertThat(orderPort.updateByIdCalls).isEqualTo(1);
        assertThat(brokerPort.calls).isEqualTo(1);
        assertThat(signalPort.requestedSignalId).isZero();
        assertThat(signalPort.savedSignal).isNull();
        assertThat(historyPort.records)
                .extracting(
                        OrderStatusHistoryRecord::fromStatus,
                        OrderStatusHistoryRecord::toStatus
                )
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(
                                OrderStatus.BROKER_FAILED,
                                OrderStatus.RETRY_REQUESTED
                        ),
                        org.assertj.core.groups.Tuple.tuple(
                                OrderStatus.RETRY_REQUESTED,
                                OrderStatus.BROKER_FAILED
                        )
                );
    }

    @Test
    void rejectsNonRetryableBrokerFailureWithoutCallingBroker() {
        RecordingOrderPort orderPort = new RecordingOrderPort(failedOrder(false));
        RecordingBrokerPort brokerPort = new RecordingBrokerPort(false);
        BrokerFailedOrderRetryService service = service(orderPort, brokerPort);

        assertThatThrownBy(() -> service.retry(ORDER_ID))
                .isInstanceOf(OrderRetryNotAllowedException.class)
                .hasMessage("Order is not retryable");
        assertThat(orderPort.claimCalls).isZero();
        assertThat(brokerPort.calls).isZero();
    }

    @Test
    void rejectsAcceptedOrderWithoutCallingBroker() {
        OrderRequest accepted = orderRequest();
        accepted.accept("FAKE-ORIGINAL");
        RecordingOrderPort orderPort = new RecordingOrderPort(accepted);
        RecordingBrokerPort brokerPort = new RecordingBrokerPort(false);
        BrokerFailedOrderRetryService service = service(orderPort, brokerPort);

        assertThatThrownBy(() -> service.retry(ORDER_ID))
                .isInstanceOf(OrderRetryNotAllowedException.class)
                .hasMessage("Only BROKER_FAILED orders can be retried");
        assertThat(brokerPort.calls).isZero();
    }

    @Test
    void rejectsMissingOrder() {
        RecordingOrderPort orderPort = new RecordingOrderPort(null);
        RecordingBrokerPort brokerPort = new RecordingBrokerPort(false);
        BrokerFailedOrderRetryService service = service(orderPort, brokerPort);

        assertThatThrownBy(() -> service.retry(999L))
                .isInstanceOf(OrderRequestNotFoundException.class)
                .hasMessage("Order request not found: 999");
        assertThat(brokerPort.calls).isZero();
    }

    @Test
    void rejectsConcurrentRetryWhenAtomicClaimFails() {
        RecordingOrderPort orderPort = new RecordingOrderPort(failedOrder(true));
        orderPort.claimResult = false;
        RecordingBrokerPort brokerPort = new RecordingBrokerPort(false);
        BrokerFailedOrderRetryService service = service(orderPort, brokerPort);

        assertThatThrownBy(() -> service.retry(ORDER_ID))
                .isInstanceOf(OrderRetryNotAllowedException.class)
                .hasMessage("Order retry is already in progress or no longer allowed");
        assertThat(orderPort.claimCalls).isEqualTo(1);
        assertThat(brokerPort.calls).isZero();
    }

    private static BrokerFailedOrderRetryService service(
            RecordingOrderPort orderPort,
            BrokerPort brokerPort
    ) {
        return service(orderPort, brokerPort, new RecordingOrderHistoryPort());
    }

    private static BrokerFailedOrderRetryService service(
            RecordingOrderPort orderPort,
            BrokerPort brokerPort,
            OrderStatusHistoryPort historyPort
    ) {
        return new BrokerFailedOrderRetryService(
                orderPort,
                brokerPort,
                emptySignalPort(),
                historyPort,
                SignalStatusHistoryPort.noop(),
                Clock.fixed(Instant.parse("2026-06-05T06:10:00Z"), ZoneOffset.UTC)
        );
    }

    private static TradingSignalPort emptySignalPort() {
        return new TradingSignalPort() {
            @Override
            public TradingSignal save(TradingSignal tradingSignal) {
                return tradingSignal;
            }

            @Override
            public Optional<TradingSignal> find(
                    String strategyName,
                    String stockCode,
                    LocalDate signalDate,
                    SignalType signalType
            ) {
                return Optional.empty();
            }

            @Override
            public Optional<TradingSignal> findById(long signalId) {
                return Optional.empty();
            }
        };
    }

    private static OrderRequest failedOrder(boolean retryable) {
        return failedOrder(retryable, null);
    }

    private static OrderRequest failedOrder(boolean retryable, Long signalId) {
        OrderRequest orderRequest = orderRequest();
        if (signalId != null) {
            orderRequest = new OrderRequest(
                    "005930",
                    OrderSide.BUY,
                    OrderType.LIMIT,
                    1,
                    BigDecimal.valueOf(50_000),
                    "CLOSING_BET",
                    LocalDate.of(2026, 6, 5),
                    signalId
            );
        }
        orderRequest.markBrokerFailed(
                "initial failure",
                Instant.parse("2026-06-05T06:01:00Z"),
                retryable
        );
        return orderRequest;
    }

    private static TradingSignal signal() {
        return new TradingSignal(
                "CLOSING_BET",
                "005930",
                LocalDate.of(2026, 6, 5),
                SignalType.BUY_CANDIDATE,
                80,
                List.of("TEST")
        );
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

    private static class RecordingOrderPort implements OrderRequestPort {
        private final OrderRequest orderRequest;
        private boolean claimResult = true;
        private int createCalls;
        private int claimCalls;
        private int updateByIdCalls;
        private long updatedOrderId;

        private RecordingOrderPort(OrderRequest orderRequest) {
            this.orderRequest = orderRequest;
        }

        @Override
        public OrderRequest create(OrderRequest orderRequest) {
            createCalls++;
            return orderRequest;
        }

        @Override
        public OrderRequest update(OrderRequest orderRequest) {
            return orderRequest;
        }

        @Override
        public OrderRequest updateById(long orderId, OrderRequest orderRequest) {
            updateByIdCalls++;
            updatedOrderId = orderId;
            return orderRequest;
        }

        @Override
        public Optional<OrderRequest> findById(long orderId) {
            return Optional.ofNullable(orderRequest);
        }

        @Override
        public boolean claimRetry(long orderId) {
            claimCalls++;
            return claimResult;
        }

        @Override
        public boolean exists(
                String stockCode,
                String strategyName,
                LocalDate tradeDate,
                OrderSide side
        ) {
            return orderRequest != null;
        }

        @Override
        public List<OrderRequestRecord> find(
                String stockCode,
                LocalDate tradeDate,
                OrderStatus status,
                OrderSide side
        ) {
            return List.of();
        }
    }

    private static class RecordingBrokerPort implements BrokerPort {
        private final boolean fail;
        private int calls;

        private RecordingBrokerPort(boolean fail) {
            this.fail = fail;
        }

        @Override
        public OrderRequest requestOrder(OrderRequest orderRequest) {
            calls++;
            if (fail) {
                throw new IllegalStateException("retry timeout");
            }
            orderRequest.markRequested();
            orderRequest.accept("FAKE-RETRY");
            return orderRequest;
        }
    }

    private static class RecordingSignalPort implements TradingSignalPort {
        private final TradingSignal signal;
        private long requestedSignalId;
        private TradingSignal savedSignal;

        private RecordingSignalPort(TradingSignal signal) {
            this.signal = signal;
        }

        @Override
        public TradingSignal save(TradingSignal tradingSignal) {
            savedSignal = tradingSignal;
            return tradingSignal;
        }

        @Override
        public Optional<TradingSignal> find(
                String strategyName,
                String stockCode,
                LocalDate signalDate,
                SignalType signalType
        ) {
            return Optional.empty();
        }

        @Override
        public Optional<TradingSignal> findById(long signalId) {
            requestedSignalId = signalId;
            return Optional.of(signal);
        }
    }

    private static class RecordingOrderHistoryPort implements OrderStatusHistoryPort {
        private final java.util.ArrayList<OrderStatusHistoryRecord> records =
                new java.util.ArrayList<>();

        @Override
        public void save(
                long orderRequestId,
                OrderStatus fromStatus,
                OrderStatus toStatus,
                String reason,
                Instant createdAt
        ) {
            records.add(new OrderStatusHistoryRecord(
                    (long) records.size() + 1,
                    orderRequestId,
                    fromStatus,
                    toStatus,
                    reason,
                    createdAt
            ));
        }

        @Override
        public List<OrderStatusHistoryRecord> findByOrderRequestId(long orderRequestId) {
            return records;
        }
    }
}
