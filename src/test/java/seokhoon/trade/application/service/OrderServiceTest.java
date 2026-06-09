package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.MockOrderResult;
import seokhoon.trade.application.port.out.BrokerPort;
import seokhoon.trade.application.port.out.DuplicateOrderRequestException;
import seokhoon.trade.application.port.out.OrderRequestPort;
import seokhoon.trade.application.port.out.OrderRequestRecord;
import seokhoon.trade.application.port.out.OrderStatusHistoryPort;
import seokhoon.trade.application.port.out.OrderStatusHistoryRecord;
import seokhoon.trade.application.port.out.SignalStatusHistoryPort;
import seokhoon.trade.application.port.out.SignalStatusHistoryRecord;
import seokhoon.trade.application.port.out.TradingSignalPort;
import seokhoon.trade.domain.order.OrderRequest;
import seokhoon.trade.domain.order.OrderSide;
import seokhoon.trade.domain.order.OrderStatus;
import seokhoon.trade.domain.risk.RiskManager;
import seokhoon.trade.domain.strategy.SignalType;
import seokhoon.trade.domain.strategy.TradingSignal;
import seokhoon.trade.domain.strategy.TradingSignalStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class OrderServiceTest {
    @Test
    void persistsRiskRejectionWithoutCallingBrokerOrSavingOrder() {
        RecordingOrderRequestPort orderPort = new RecordingOrderRequestPort(false);
        RecordingTradingSignalPort signalPort = new RecordingTradingSignalPort();
        RecordingBrokerPort brokerPort = new RecordingBrokerPort();
        OrderService service = new OrderService(orderPort, signalPort, brokerPort, new RiskManager());

        MockOrderResult result = service.request(signal(69), 1, BigDecimal.valueOf(50_000));

        assertThat(result.riskDecision().approved()).isFalse();
        assertThat(result.riskDecision().reasons()).containsExactly("SCORE_BELOW_70");
        assertThat(result.orderRequest()).isNull();
        assertThat(result.tradingSignal().status()).isEqualTo(TradingSignalStatus.RISK_REJECTED);
        assertThat(result.tradingSignal().riskReasons()).containsExactly("SCORE_BELOW_70");
        assertThat(signalPort.savedStatuses).containsExactly(TradingSignalStatus.RISK_REJECTED);
        assertThat(brokerPort.calls).isZero();
        assertThat(orderPort.createCalls).isZero();
        assertThat(orderPort.updateCalls).isZero();
    }

    @Test
    void persistsAcceptedOrderAndMarksSignalAsOrderRequested() {
        RecordingOrderRequestPort orderPort = new RecordingOrderRequestPort(false);
        RecordingTradingSignalPort signalPort = new RecordingTradingSignalPort();
        RecordingBrokerPort brokerPort = new RecordingBrokerPort();
        RecordingSignalHistoryPort signalHistory = new RecordingSignalHistoryPort();
        RecordingOrderHistoryPort orderHistory = new RecordingOrderHistoryPort();
        OrderService service = new OrderService(
                orderPort,
                signalPort,
                brokerPort,
                new RiskManager(),
                signalHistory,
                orderHistory,
                Clock.fixed(Instant.parse("2026-06-05T06:01:00Z"), ZoneOffset.UTC)
        );

        MockOrderResult result = service.request(signal(80), 21L, 1, BigDecimal.valueOf(50_000));

        assertThat(result.riskDecision().approved()).isTrue();
        assertThat(result.orderRequest()).isNotNull();
        assertThat(result.tradingSignal().status()).isEqualTo(TradingSignalStatus.ORDER_REQUESTED);
        assertThat(signalPort.savedStatuses)
                .containsExactly(TradingSignalStatus.RISK_APPROVED, TradingSignalStatus.ORDER_REQUESTED);
        assertThat(brokerPort.calls).isEqualTo(1);
        assertThat(orderPort.createCalls).isEqualTo(1);
        assertThat(orderPort.updateCalls).isEqualTo(1);
        assertThat(signalHistory.records)
                .extracting(
                        SignalStatusHistoryRecord::fromStatus,
                        SignalStatusHistoryRecord::toStatus
                )
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(
                                TradingSignalStatus.CREATED,
                                TradingSignalStatus.RISK_APPROVED
                        ),
                        org.assertj.core.groups.Tuple.tuple(
                                TradingSignalStatus.RISK_APPROVED,
                                TradingSignalStatus.ORDER_REQUESTED
                        )
                );
        assertThat(orderHistory.records)
                .singleElement()
                .satisfies(history -> {
                    assertThat(history.orderRequestId()).isEqualTo(10L);
                    assertThat(history.fromStatus()).isEqualTo(OrderStatus.CREATED);
                    assertThat(history.toStatus()).isEqualTo(OrderStatus.ACCEPTED);
                });
    }

    @Test
    void convertsDatabaseDuplicateIntoRiskRejectionBeforeCallingBroker() {
        RecordingOrderRequestPort orderPort = new RecordingOrderRequestPort(false);
        orderPort.failCreateAsDuplicate = true;
        RecordingTradingSignalPort signalPort = new RecordingTradingSignalPort();
        RecordingBrokerPort brokerPort = new RecordingBrokerPort();
        OrderService service = new OrderService(orderPort, signalPort, brokerPort, new RiskManager());

        MockOrderResult result = service.request(signal(80), 1, BigDecimal.valueOf(50_000));

        assertThat(result.riskDecision().approved()).isFalse();
        assertThat(result.riskDecision().reasons()).containsExactly("DUPLICATE_ORDER");
        assertThat(result.tradingSignal().status()).isEqualTo(TradingSignalStatus.RISK_REJECTED);
        assertThat(result.tradingSignal().riskReasons()).containsExactly("DUPLICATE_ORDER");
        assertThat(signalPort.savedStatuses)
                .containsExactly(TradingSignalStatus.RISK_APPROVED, TradingSignalStatus.RISK_REJECTED);
        assertThat(orderPort.createCalls).isEqualTo(1);
        assertThat(orderPort.updateCalls).isZero();
        assertThat(brokerPort.calls).isZero();
        assertThat(result.brokerFailed()).isFalse();
    }

    @Test
    void persistsBrokerFailureWithoutMarkingSignalAsOrderRequested() {
        RecordingOrderRequestPort orderPort = new RecordingOrderRequestPort(false);
        RecordingTradingSignalPort signalPort = new RecordingTradingSignalPort();
        BrokerPort failingBroker = orderRequest -> {
            throw new IllegalStateException("broker timeout");
        };
        Clock clock = Clock.fixed(Instant.parse("2026-06-05T06:01:00Z"), ZoneOffset.UTC);
        RecordingSignalHistoryPort signalHistory = new RecordingSignalHistoryPort();
        RecordingOrderHistoryPort orderHistory = new RecordingOrderHistoryPort();
        OrderService service = new OrderService(
                orderPort,
                signalPort,
                failingBroker,
                new RiskManager(),
                signalHistory,
                orderHistory,
                clock
        );

        MockOrderResult result = service.request(signal(80), 21L, 1, BigDecimal.valueOf(50_000));

        assertThat(result.riskDecision().approved()).isTrue();
        assertThat(result.brokerFailed()).isTrue();
        assertThat(result.failureReason()).isEqualTo("broker timeout");
        assertThat(result.orderRequest().status()).isEqualTo(OrderStatus.BROKER_FAILED);
        assertThat(result.orderRequest().brokerOrderNo()).isNull();
        assertThat(result.orderRequest().failedAt())
                .isEqualTo(Instant.parse("2026-06-05T06:01:00Z"));
        assertThat(result.orderRequest().retryable()).isTrue();
        assertThat(result.tradingSignal().status()).isEqualTo(TradingSignalStatus.RISK_APPROVED);
        assertThat(signalPort.savedStatuses).containsExactly(TradingSignalStatus.RISK_APPROVED);
        assertThat(orderPort.createCalls).isEqualTo(1);
        assertThat(orderPort.updateCalls).isEqualTo(1);
        assertThat(orderPort.lastUpdated.status()).isEqualTo(OrderStatus.BROKER_FAILED);
        assertThat(signalHistory.records)
                .singleElement()
                .satisfies(history -> {
                    assertThat(history.fromStatus()).isEqualTo(TradingSignalStatus.CREATED);
                    assertThat(history.toStatus()).isEqualTo(TradingSignalStatus.RISK_APPROVED);
                });
        assertThat(orderHistory.records)
                .singleElement()
                .satisfies(history -> {
                    assertThat(history.fromStatus()).isEqualTo(OrderStatus.CREATED);
                    assertThat(history.toStatus()).isEqualTo(OrderStatus.BROKER_FAILED);
                    assertThat(history.reason()).isEqualTo("broker timeout");
                });
    }

    private static TradingSignal signal(int score) {
        return new TradingSignal(
                "CLOSING_BET",
                "005930",
                LocalDate.of(2026, 6, 5),
                SignalType.BUY_CANDIDATE,
                score,
                List.of("TEST")
        );
    }

    private static class RecordingOrderRequestPort implements OrderRequestPort {
        private final boolean existing;
        private int createCalls;
        private int updateCalls;
        private boolean failCreateAsDuplicate;
        private OrderRequest lastUpdated;

        private RecordingOrderRequestPort(boolean existing) {
            this.existing = existing;
        }

        @Override
        public OrderRequest create(OrderRequest orderRequest) {
            createCalls++;
            if (failCreateAsDuplicate) {
                throw new DuplicateOrderRequestException(new IllegalStateException("duplicate"));
            }
            return orderRequest;
        }

        @Override
        public OrderRequest update(OrderRequest orderRequest) {
            updateCalls++;
            lastUpdated = orderRequest;
            return orderRequest;
        }

        @Override
        public OrderRequest updateById(long orderId, OrderRequest orderRequest) {
            return update(orderRequest);
        }

        @Override
        public Optional<OrderRequest> findById(long orderId) {
            return Optional.empty();
        }

        @Override
        public Optional<Long> findId(OrderRequest orderRequest) {
            return Optional.of(10L);
        }

        @Override
        public boolean claimRetry(long orderId) {
            return false;
        }

        @Override
        public boolean exists(
                String stockCode,
                String strategyName,
                LocalDate tradeDate,
                OrderSide side
        ) {
            return existing;
        }

        @Override
        public List<OrderRequestRecord> find(
                String stockCode,
                LocalDate tradeDate,
                seokhoon.trade.domain.order.OrderStatus status,
                OrderSide side
        ) {
            return List.of();
        }
    }

    private static class RecordingTradingSignalPort implements TradingSignalPort {
        private final List<TradingSignalStatus> savedStatuses = new ArrayList<>();

        @Override
        public TradingSignal save(TradingSignal tradingSignal) {
            savedStatuses.add(tradingSignal.status());
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

        @Override
        public Optional<Long> findId(
                String strategyName,
                String stockCode,
                LocalDate signalDate,
                SignalType signalType
        ) {
            return Optional.of(21L);
        }
    }

    private static class RecordingBrokerPort implements BrokerPort {
        private int calls;

        @Override
        public OrderRequest requestOrder(OrderRequest orderRequest) {
            calls++;
            orderRequest.markRequested();
            orderRequest.accept("FAKE-ORDER");
            return orderRequest;
        }
    }

    private static class RecordingSignalHistoryPort implements SignalStatusHistoryPort {
        private final List<SignalStatusHistoryRecord> records = new ArrayList<>();

        @Override
        public void save(
                long tradingSignalId,
                TradingSignalStatus fromStatus,
                TradingSignalStatus toStatus,
                String reason,
                Instant createdAt
        ) {
            records.add(new SignalStatusHistoryRecord(
                    (long) records.size() + 1,
                    tradingSignalId,
                    fromStatus,
                    toStatus,
                    reason,
                    createdAt
            ));
        }

        @Override
        public List<SignalStatusHistoryRecord> findByTradingSignalId(long tradingSignalId) {
            return records;
        }
    }

    private static class RecordingOrderHistoryPort implements OrderStatusHistoryPort {
        private final List<OrderStatusHistoryRecord> records = new ArrayList<>();

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
