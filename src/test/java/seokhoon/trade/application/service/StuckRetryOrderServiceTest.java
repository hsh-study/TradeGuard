package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.OrderRequestView;
import seokhoon.trade.application.port.out.OrderRequestPort;
import seokhoon.trade.application.port.out.OrderRequestRecord;
import seokhoon.trade.application.port.out.OrderStatusHistoryPort;
import seokhoon.trade.application.port.out.OrderStatusHistoryRecord;
import seokhoon.trade.domain.order.OrderRequest;
import seokhoon.trade.domain.audit.AuditActor;
import seokhoon.trade.domain.order.OrderSide;
import seokhoon.trade.domain.order.OrderStatus;
import seokhoon.trade.domain.order.OrderType;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StuckRetryOrderServiceTest {
    private static final Instant NOW = Instant.parse("2026-06-05T06:10:00Z");
    private static final Duration THRESHOLD = Duration.ofMinutes(5);

    @Test
    void loadsOnlyRetryRequestedOrdersThatExceededThreshold() {
        RecordingOrderPort port = new RecordingOrderPort(null);
        port.records.add(record(1L, OrderStatus.RETRY_REQUESTED, NOW.minusSeconds(301)));
        port.records.add(record(2L, OrderStatus.RETRY_REQUESTED, NOW.minusSeconds(299)));
        port.records.add(record(3L, OrderStatus.ACCEPTED, NOW.minusSeconds(600)));
        port.records.add(record(4L, OrderStatus.BROKER_FAILED, NOW.minusSeconds(600)));
        StuckRetryOrderService service = new StuckRetryOrderService(port);

        List<OrderRequestView> result = service.load(NOW, THRESHOLD);

        assertThat(result).extracting(OrderRequestView::orderId).containsExactly(1L);
        assertThat(port.cutoff).isEqualTo(NOW.minus(THRESHOLD));
    }

    @Test
    void recoversStuckRetryAsBrokerFailedAndKeepsItRetryable() {
        OrderRequest order = retryRequestedOrder(NOW.minusSeconds(301));
        RecordingOrderPort port = new RecordingOrderPort(order);
        RecordingHistoryPort historyPort = new RecordingHistoryPort();
        StuckRetryOrderService service = new StuckRetryOrderService(port, historyPort);

        OrderRequestView result = service.recover(
                10L,
                "application restarted during retry",
                NOW,
                THRESHOLD
        );

        assertThat(result.status()).isEqualTo(OrderStatus.BROKER_FAILED);
        assertThat(result.retryable()).isTrue();
        assertThat(result.retryRequestedAt()).isNull();
        assertThat(result.failedAt()).isEqualTo(NOW);
        assertThat(result.failureReason())
                .isEqualTo("Retry request stuck recovered: application restarted during retry");
        assertThat(port.recoveredOrderId).isEqualTo(10L);
        assertThat(historyPort.records)
                .singleElement()
                .satisfies(history -> {
                    assertThat(history.fromStatus()).isEqualTo(OrderStatus.RETRY_REQUESTED);
                    assertThat(history.toStatus()).isEqualTo(OrderStatus.BROKER_FAILED);
                    assertThat(history.reason()).startsWith("Retry request stuck recovered:");
                    assertThat(history.actor()).isEqualTo(AuditActor.SYSTEM);
                    assertThat(history.requestCorrelationId()).isNotBlank();
                });
    }

    @Test
    void rejectsRetryRequestedOrderThatHasNotExceededThreshold() {
        RecordingOrderPort port = new RecordingOrderPort(
                retryRequestedOrder(NOW.minusSeconds(299))
        );
        StuckRetryOrderService service = new StuckRetryOrderService(port);

        assertThatThrownBy(() -> service.recover(10L, "restart", NOW, THRESHOLD))
                .isInstanceOf(StuckRetryRecoveryNotAllowedException.class)
                .hasMessage("Order retry has not exceeded the stuck threshold");
    }

    @Test
    void rejectsNonRetryRequestedOrder() {
        RecordingOrderPort port = new RecordingOrderPort(failedOrder());
        StuckRetryOrderService service = new StuckRetryOrderService(port);

        assertThatThrownBy(() -> service.recover(10L, "restart", NOW, THRESHOLD))
                .isInstanceOf(StuckRetryRecoveryNotAllowedException.class)
                .hasMessage("Only RETRY_REQUESTED orders can be recovered");
    }

    @Test
    void rejectsRecoveryWhenConditionalUpdateLosesRace() {
        RecordingOrderPort port = new RecordingOrderPort(
                retryRequestedOrder(NOW.minusSeconds(301))
        );
        port.recoveryResult = false;
        StuckRetryOrderService service = new StuckRetryOrderService(port);

        assertThatThrownBy(() -> service.recover(10L, "restart", NOW, THRESHOLD))
                .isInstanceOf(StuckRetryRecoveryNotAllowedException.class)
                .hasMessage("Order retry is no longer recoverable");
    }

    @Test
    void rejectsMissingOrder() {
        StuckRetryOrderService service = new StuckRetryOrderService(
                new RecordingOrderPort(null)
        );

        assertThatThrownBy(() -> service.recover(999L, "restart", NOW, THRESHOLD))
                .isInstanceOf(OrderRequestNotFoundException.class)
                .hasMessage("Order request not found: 999");
    }

    private static OrderRequest retryRequestedOrder(Instant requestedAt) {
        OrderRequest order = failedOrder();
        order.markRetryRequested(requestedAt);
        return order;
    }

    private static OrderRequest failedOrder() {
        OrderRequest order = new OrderRequest(
                "005930",
                OrderSide.BUY,
                OrderType.LIMIT,
                1,
                BigDecimal.valueOf(50_000),
                "CLOSING_BET",
                LocalDate.of(2026, 6, 5),
                21L
        );
        order.markBrokerFailed("timeout", NOW.minusSeconds(600), true);
        return order;
    }

    private static OrderRequestRecord record(
            long id,
            OrderStatus status,
            Instant retryRequestedAt
    ) {
        return new OrderRequestRecord(
                id,
                "005930",
                OrderSide.BUY,
                OrderType.LIMIT,
                1,
                BigDecimal.valueOf(50_000),
                status,
                null,
                null,
                null,
                true,
                "CLOSING_BET",
                LocalDate.of(2026, 6, 5),
                21L,
                retryRequestedAt
        );
    }

    private static class RecordingOrderPort implements OrderRequestPort {
        private final OrderRequest order;
        private final List<OrderRequestRecord> records = new ArrayList<>();
        private Instant cutoff;
        private long recoveredOrderId;
        private boolean recoveryResult = true;

        private RecordingOrderPort(OrderRequest order) {
            this.order = order;
        }

        @Override
        public OrderRequest create(OrderRequest orderRequest) {
            return orderRequest;
        }

        @Override
        public OrderRequest update(OrderRequest orderRequest) {
            return orderRequest;
        }

        @Override
        public OrderRequest updateById(long orderId, OrderRequest orderRequest) {
            return orderRequest;
        }

        @Override
        public Optional<OrderRequest> findById(long orderId) {
            return Optional.ofNullable(order);
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
            return false;
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

        @Override
        public List<OrderRequestRecord> findStuckRetries(Instant cutoff) {
            this.cutoff = cutoff;
            return records.stream()
                    .filter(record -> record.status() == OrderStatus.RETRY_REQUESTED)
                    .filter(record -> record.retryRequestedAt() != null)
                    .filter(record -> !record.retryRequestedAt().isAfter(cutoff))
                    .toList();
        }

        @Override
        public boolean recoverStuckRetry(
                long orderId,
                Instant cutoff,
                OrderRequest recoveredOrder
        ) {
            recoveredOrderId = orderId;
            return recoveryResult;
        }
    }

    private static class RecordingHistoryPort implements OrderStatusHistoryPort {
        private final List<OrderStatusHistoryRecord> records = new ArrayList<>();

        @Override
        public void save(
                long orderRequestId,
                OrderStatus fromStatus,
                OrderStatus toStatus,
                String reason,
                AuditActor actor,
                String requestCorrelationId,
                Instant createdAt
        ) {
            records.add(new OrderStatusHistoryRecord(
                    (long) records.size() + 1,
                    orderRequestId,
                    fromStatus,
                    toStatus,
                    reason,
                    actor,
                    requestCorrelationId,
                    createdAt
            ));
        }

        @Override
        public List<OrderStatusHistoryRecord> findByOrderRequestId(long orderRequestId) {
            return records;
        }
    }
}
