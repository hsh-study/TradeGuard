package seokhoon.trade.adapter.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import seokhoon.trade.application.port.out.DuplicateOrderRequestException;
import seokhoon.trade.application.port.out.OrderRequestPort;
import seokhoon.trade.application.port.out.OrderRequestRecord;
import seokhoon.trade.application.port.out.OrderStatusHistoryPort;
import seokhoon.trade.application.port.out.SignalStatusHistoryPort;
import seokhoon.trade.application.port.in.RetryBrokerFailedOrderUseCase;
import seokhoon.trade.application.port.in.RequestSignalMockOrderUseCase;
import seokhoon.trade.application.port.in.RequestStoredMockOrderUseCase;
import seokhoon.trade.application.port.in.SignalMockOrderCommand;
import seokhoon.trade.application.port.in.StoredMockOrderCommand;
import seokhoon.trade.domain.order.OrderRequest;
import seokhoon.trade.domain.order.OrderSide;
import seokhoon.trade.domain.order.OrderStatus;
import seokhoon.trade.domain.order.OrderType;
import seokhoon.trade.application.port.out.TradingSignalPort;
import seokhoon.trade.domain.strategy.SignalType;
import seokhoon.trade.domain.strategy.TradingSignal;
import seokhoon.trade.domain.strategy.TradingSignalStatus;

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

    @Autowired
    private RequestSignalMockOrderUseCase requestSignalMockOrderUseCase;

    @Autowired
    private RequestStoredMockOrderUseCase requestStoredMockOrderUseCase;

    @Autowired
    private TradingSignalPort tradingSignalPort;

    @Autowired
    private TradingSignalJpaRepository tradingSignalRepository;

    @Autowired
    private OrderRequestStatusHistoryJpaRepository orderHistoryRepository;

    @Autowired
    private TradingSignalStatusHistoryJpaRepository signalHistoryRepository;

    @Autowired
    private OrderStatusHistoryPort orderStatusHistoryPort;

    @Autowired
    private SignalStatusHistoryPort signalStatusHistoryPort;

    @BeforeEach
    void clearOrders() {
        orderHistoryRepository.deleteAll();
        signalHistoryRepository.deleteAll();
        repository.deleteAll();
        tradingSignalRepository.deleteAll();
    }

    @AfterEach
    void cleanUpOrders() {
        clearOrders();
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

        Instant retryRequestedAt = Instant.parse("2026-06-05T06:05:00Z");
        boolean firstClaim = orderRequestPort.claimRetry(orderId, retryRequestedAt);
        boolean secondClaim = orderRequestPort.claimRetry(orderId, retryRequestedAt);

        assertThat(firstClaim).isTrue();
        assertThat(secondClaim).isFalse();
        assertThat(orderRequestPort.findById(orderId).orElseThrow().retryRequestedAt())
                .isEqualTo(retryRequestedAt);
        assertThat(orderRequestPort.findStuckRetries(retryRequestedAt.minusSeconds(1)))
                .isEmpty();
        assertThat(orderRequestPort.findStuckRetries(retryRequestedAt))
                .singleElement()
                .extracting(OrderRequestRecord::id)
                .isEqualTo(orderId);
        assertThat(repository.count()).isEqualTo(1);
        assertThat(orderRequestPort.findById(orderId))
                .hasValueSatisfying(order ->
                        assertThat(order.status()).isEqualTo(OrderStatus.RETRY_REQUESTED));
    }

    @Test
    void conditionallyRecoversStuckRetryWithoutCreatingNewRow() {
        OrderRequest failed = orderRequest();
        orderRequestPort.create(failed);
        failed.markBrokerFailed(
                "broker timeout",
                Instant.parse("2026-06-05T06:01:00Z"),
                true
        );
        orderRequestPort.update(failed);
        long orderId = repository.findAll().getFirst().toRecord().id();
        Instant requestedAt = Instant.parse("2026-06-05T06:05:00Z");
        orderRequestPort.claimRetry(orderId, requestedAt);
        OrderRequest recovered = orderRequestPort.findById(orderId).orElseThrow();
        recovered.markRetryStuckRecovered(
                "application restarted during retry",
                Instant.parse("2026-06-05T06:11:00Z")
        );

        boolean firstRecovery = orderRequestPort.recoverStuckRetry(
                orderId,
                Instant.parse("2026-06-05T06:06:00Z"),
                recovered
        );
        boolean secondRecovery = orderRequestPort.recoverStuckRetry(
                orderId,
                Instant.parse("2026-06-05T06:06:00Z"),
                recovered
        );

        assertThat(firstRecovery).isTrue();
        assertThat(secondRecovery).isFalse();
        assertThat(repository.count()).isEqualTo(1);
        assertThat(orderRequestPort.findById(orderId)).hasValueSatisfying(order -> {
            assertThat(order.status()).isEqualTo(OrderStatus.BROKER_FAILED);
            assertThat(order.retryable()).isTrue();
            assertThat(order.retryRequestedAt()).isNull();
            assertThat(order.failureReason()).startsWith("Retry request stuck recovered:");
        });
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
        assertThat(orderStatusHistoryPort.findByOrderRequestId(orderId))
                .extracting(history -> history.toStatus())
                .containsExactly(OrderStatus.RETRY_REQUESTED, OrderStatus.ACCEPTED);
        assertThat(orderRequestPort.findById(orderId))
                .hasValueSatisfying(order -> {
                    assertThat(order.status()).isEqualTo(OrderStatus.ACCEPTED);
                    assertThat(order.brokerOrderNo()).startsWith("FAKE-");
                });
    }

    @Test
    void storesSignalIdForSignalIdBasedMockOrder() {
        TradingSignal signal = signal("035420", LocalDate.of(2026, 6, 9));
        tradingSignalPort.save(signal);
        long signalId = tradingSignalPort.findId(
                signal.strategyName(),
                signal.stockCode(),
                signal.signalDate(),
                signal.signalType()
        ).orElseThrow();

        requestSignalMockOrderUseCase.request(
                signalId,
                new SignalMockOrderCommand(1, BigDecimal.valueOf(50_000))
        );

        assertThat(repository.findAll())
                .singleElement()
                .extracting(OrderRequestEntity::signalId)
                .isEqualTo(signalId);
        long orderId = repository.findAll().getFirst().id();
        assertThat(signalStatusHistoryPort.findByTradingSignalId(signalId))
                .extracting(history -> history.toStatus())
                .containsExactly(
                        TradingSignalStatus.RISK_APPROVED,
                        TradingSignalStatus.ORDER_REQUESTED
                );
        assertThat(orderStatusHistoryPort.findByOrderRequestId(orderId))
                .singleElement()
                .satisfies(history -> {
                    assertThat(history.fromStatus()).isEqualTo(OrderStatus.CREATED);
                    assertThat(history.toStatus()).isEqualTo(OrderStatus.ACCEPTED);
                });
    }

    @Test
    void storesSignalIdForLogicalKeyMockOrder() {
        TradingSignal signal = signal("068270", LocalDate.of(2026, 6, 10));
        tradingSignalPort.save(signal);
        long signalId = tradingSignalPort.findId(
                signal.strategyName(),
                signal.stockCode(),
                signal.signalDate(),
                signal.signalType()
        ).orElseThrow();

        requestStoredMockOrderUseCase.request(new StoredMockOrderCommand(
                signal.strategyName(),
                signal.stockCode(),
                signal.signalDate(),
                signal.signalType(),
                1,
                BigDecimal.valueOf(50_000)
        ));

        assertThat(repository.findAll())
                .singleElement()
                .extracting(OrderRequestEntity::signalId)
                .isEqualTo(signalId);
    }

    @Test
    void synchronizesLinkedSignalAfterSuccessfulRetry() {
        TradingSignal signal = signal("207940", LocalDate.of(2026, 6, 11));
        tradingSignalPort.save(signal);
        long signalId = tradingSignalPort.findId(
                signal.strategyName(),
                signal.stockCode(),
                signal.signalDate(),
                signal.signalType()
        ).orElseThrow();
        OrderRequest failed = new OrderRequest(
                signal.stockCode(),
                OrderSide.BUY,
                OrderType.LIMIT,
                1,
                BigDecimal.valueOf(50_000),
                signal.strategyName(),
                signal.signalDate(),
                signalId
        );
        orderRequestPort.create(failed);
        failed.markBrokerFailed(
                "broker timeout",
                Instant.parse("2026-06-11T06:01:00Z"),
                true
        );
        orderRequestPort.update(failed);
        long orderId = repository.findAll().getFirst().toRecord().id();

        retryBrokerFailedOrderUseCase.retry(orderId);

        assertThat(tradingSignalPort.findById(signalId))
                .hasValueSatisfying(stored ->
                        assertThat(stored.status()).isEqualTo(TradingSignalStatus.ORDER_REQUESTED));
    }

    @Test
    void upsertsEarlyMarketSignalsBySignalTypeWithoutMixingStages() {
        LocalDate tradeDate = LocalDate.of(2026, 6, 10);
        TradingSignal preScan = new TradingSignal(
                "EARLY_MARKET_BREAKOUT",
                "005930",
                tradeDate,
                SignalType.EARLY_MARKET_PRE_SCAN,
                80,
                java.util.List.of("TRADING_VALUE_TOP")
        );
        TradingSignal entry = new TradingSignal(
                "EARLY_MARKET_BREAKOUT",
                "005930",
                tradeDate,
                SignalType.EARLY_MARKET_ENTRY_CANDIDATE,
                90,
                java.util.List.of("ABOVE_VWAP")
        );

        tradingSignalPort.save(preScan);
        tradingSignalPort.save(new TradingSignal(
                "EARLY_MARKET_BREAKOUT",
                "005930",
                tradeDate,
                SignalType.EARLY_MARKET_PRE_SCAN,
                85,
                java.util.List.of("TRADING_VALUE_TOP", "VOLUME_TOP")
        ));
        tradingSignalPort.save(entry);

        assertThat(tradingSignalRepository.count()).isEqualTo(2);
        assertThat(tradingSignalPort.find(
                "EARLY_MARKET_BREAKOUT",
                "005930",
                tradeDate,
                SignalType.EARLY_MARKET_PRE_SCAN
        )).hasValueSatisfying(signal -> assertThat(signal.score()).isEqualTo(85));
        assertThat(tradingSignalPort.find(
                "EARLY_MARKET_BREAKOUT",
                "005930",
                tradeDate,
                SignalType.EARLY_MARKET_ENTRY_CANDIDATE
        )).hasValueSatisfying(signal -> assertThat(signal.score()).isEqualTo(90));
    }

    @Test
    void requestsMockLimitOrderFromEarlyMarketEntryCandidate() {
        LocalDate tradeDate = LocalDate.of(2026, 6, 10);
        TradingSignal entry = new TradingSignal(
                "EARLY_MARKET_BREAKOUT",
                "035420",
                tradeDate,
                SignalType.EARLY_MARKET_ENTRY_CANDIDATE,
                90,
                java.util.List.of("ABOVE_VWAP", "NEAR_INTRADAY_HIGH")
        );
        tradingSignalPort.save(entry);
        long signalId = tradingSignalPort.findId(
                entry.strategyName(),
                entry.stockCode(),
                entry.signalDate(),
                entry.signalType()
        ).orElseThrow();

        var result = requestSignalMockOrderUseCase.request(
                signalId,
                new SignalMockOrderCommand(1, BigDecimal.valueOf(50_000))
        );

        assertThat(result.riskDecision().approved()).isTrue();
        assertThat(result.orderRequest().orderType()).isEqualTo(OrderType.LIMIT);
        assertThat(result.orderRequest().signalId()).isEqualTo(signalId);
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

    private static TradingSignal signal(String stockCode, LocalDate signalDate) {
        return new TradingSignal(
                "CLOSING_BET",
                stockCode,
                signalDate,
                SignalType.BUY_CANDIDATE,
                80,
                java.util.List.of("TEST")
        );
    }
}
