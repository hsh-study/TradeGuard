package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.BrokerOrderRetryResult;
import seokhoon.trade.application.port.out.BrokerPort;
import seokhoon.trade.application.port.out.OrderRequestPort;
import seokhoon.trade.application.port.out.OrderRequestRecord;
import seokhoon.trade.domain.order.OrderRequest;
import seokhoon.trade.domain.order.OrderSide;
import seokhoon.trade.domain.order.OrderStatus;
import seokhoon.trade.domain.order.OrderType;

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
        BrokerFailedOrderRetryService service = service(orderPort, brokerPort);

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
    }

    @Test
    void keepsBrokerFailedAndRefreshesFailureDetailsWhenRetryFails() {
        RecordingOrderPort orderPort = new RecordingOrderPort(failedOrder(true));
        RecordingBrokerPort brokerPort = new RecordingBrokerPort(true);
        BrokerFailedOrderRetryService service = service(orderPort, brokerPort);

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
        return new BrokerFailedOrderRetryService(
                orderPort,
                brokerPort,
                Clock.fixed(Instant.parse("2026-06-05T06:10:00Z"), ZoneOffset.UTC)
        );
    }

    private static OrderRequest failedOrder(boolean retryable) {
        OrderRequest orderRequest = orderRequest();
        orderRequest.markBrokerFailed(
                "initial failure",
                Instant.parse("2026-06-05T06:01:00Z"),
                retryable
        );
        return orderRequest;
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
}
