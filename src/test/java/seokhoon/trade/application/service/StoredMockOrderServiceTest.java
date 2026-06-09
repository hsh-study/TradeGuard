package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.MockOrderResult;
import seokhoon.trade.application.port.in.RequestMockOrderUseCase;
import seokhoon.trade.application.port.in.StoredMockOrderCommand;
import seokhoon.trade.application.port.out.TradingSignalPort;
import seokhoon.trade.domain.order.OrderRequest;
import seokhoon.trade.domain.risk.RiskDecision;
import seokhoon.trade.domain.strategy.SignalType;
import seokhoon.trade.domain.strategy.TradingSignal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StoredMockOrderServiceTest {
    private static final LocalDate SIGNAL_DATE = LocalDate.of(2026, 6, 5);

    @Test
    void loadsStoredSignalBeforeRequestingMockOrder() {
        TradingSignal signal = signal();
        RecordingTradingSignalPort signalPort = new RecordingTradingSignalPort(Optional.of(signal));
        RecordingOrderUseCase orderUseCase = new RecordingOrderUseCase();
        StoredMockOrderService service = new StoredMockOrderService(signalPort, orderUseCase);

        MockOrderResult result = service.request(command());

        assertThat(signalPort.requestedStrategyName).isEqualTo("CLOSING_BET");
        assertThat(signalPort.requestedStockCode).isEqualTo("005930");
        assertThat(orderUseCase.signal).isSameAs(signal);
        assertThat(orderUseCase.signalId).isEqualTo(7L);
        assertThat(orderUseCase.quantity).isEqualTo(1);
        assertThat(orderUseCase.limitPrice).isEqualByComparingTo("50000");
        assertThat(result.riskDecision().approved()).isTrue();
    }

    @Test
    void rejectsRequestWhenStoredSignalDoesNotExist() {
        RecordingOrderUseCase orderUseCase = new RecordingOrderUseCase();
        StoredMockOrderService service = new StoredMockOrderService(
                new RecordingTradingSignalPort(Optional.empty()),
                orderUseCase
        );

        assertThatThrownBy(() -> service.request(command()))
                .isInstanceOf(TradingSignalNotFoundException.class)
                .hasMessage("Trading signal not found");
        assertThat(orderUseCase.signal).isNull();
    }

    private static StoredMockOrderCommand command() {
        return new StoredMockOrderCommand(
                "CLOSING_BET",
                "005930",
                SIGNAL_DATE,
                SignalType.BUY_CANDIDATE,
                1,
                BigDecimal.valueOf(50_000)
        );
    }

    private static TradingSignal signal() {
        return new TradingSignal(
                "CLOSING_BET",
                "005930",
                SIGNAL_DATE,
                SignalType.BUY_CANDIDATE,
                80,
                List.of("TEST")
        );
    }

    private static class RecordingTradingSignalPort implements TradingSignalPort {
        private final Optional<TradingSignal> result;
        private String requestedStrategyName;
        private String requestedStockCode;

        private RecordingTradingSignalPort(Optional<TradingSignal> result) {
            this.result = result;
        }

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
            requestedStrategyName = strategyName;
            requestedStockCode = stockCode;
            return result;
        }

        @Override
        public Optional<TradingSignal> findById(long signalId) {
            return result;
        }

        @Override
        public Optional<Long> findId(
                String strategyName,
                String stockCode,
                LocalDate signalDate,
                SignalType signalType
        ) {
            return result.isPresent() ? Optional.of(7L) : Optional.empty();
        }
    }

    private static class RecordingOrderUseCase implements RequestMockOrderUseCase {
        private TradingSignal signal;
        private Long signalId;
        private int quantity;
        private BigDecimal limitPrice;

        @Override
        public MockOrderResult request(TradingSignal signal, int quantity, BigDecimal limitPrice) {
            return request(signal, null, quantity, limitPrice);
        }

        @Override
        public MockOrderResult request(
                TradingSignal signal,
                Long signalId,
                int quantity,
                BigDecimal limitPrice
        ) {
            this.signal = signal;
            this.signalId = signalId;
            this.quantity = quantity;
            this.limitPrice = limitPrice;
            return MockOrderResult.accepted(
                    RiskDecision.approve(),
                    signal,
                    new OrderRequest(
                            signal.stockCode(),
                            seokhoon.trade.domain.order.OrderSide.BUY,
                            seokhoon.trade.domain.order.OrderType.LIMIT,
                            quantity,
                            limitPrice,
                            signal.strategyName(),
                            signal.signalDate()
                    )
            );
        }
    }
}
