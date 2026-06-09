package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.MockOrderResult;
import seokhoon.trade.application.port.in.RequestMockOrderUseCase;
import seokhoon.trade.application.port.in.SignalMockOrderCommand;
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

class SignalMockOrderServiceTest {
    private static final LocalDate SIGNAL_DATE = LocalDate.of(2026, 6, 5);

    @Test
    void requestsMockOrderBySignalId() {
        TradingSignal signal = signal();
        RecordingTradingSignalPort signalPort = new RecordingTradingSignalPort(Optional.of(signal));
        RecordingOrderUseCase orderUseCase = new RecordingOrderUseCase();
        SignalMockOrderService service = new SignalMockOrderService(signalPort, orderUseCase);

        MockOrderResult result = service.request(1L, command());

        assertThat(signalPort.requestedSignalId).isEqualTo(1L);
        assertThat(orderUseCase.signal).isSameAs(signal);
        assertThat(orderUseCase.signalId).isEqualTo(1L);
        assertThat(result.riskDecision().approved()).isTrue();
    }

    @Test
    void rejectsMissingSignalIdWithoutRequestingOrder() {
        RecordingOrderUseCase orderUseCase = new RecordingOrderUseCase();
        SignalMockOrderService service = new SignalMockOrderService(
                new RecordingTradingSignalPort(Optional.empty()),
                orderUseCase
        );

        assertThatThrownBy(() -> service.request(999L, command()))
                .isInstanceOf(TradingSignalNotFoundException.class);
        assertThat(orderUseCase.signal).isNull();
    }

    private static SignalMockOrderCommand command() {
        return new SignalMockOrderCommand(1, BigDecimal.valueOf(50_000));
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
        private final Optional<TradingSignal> signal;
        private long requestedSignalId;

        private RecordingTradingSignalPort(Optional<TradingSignal> signal) {
            this.signal = signal;
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
            return Optional.empty();
        }

        @Override
        public Optional<TradingSignal> findById(long signalId) {
            requestedSignalId = signalId;
            return signal;
        }
    }

    private static class RecordingOrderUseCase implements RequestMockOrderUseCase {
        private TradingSignal signal;
        private Long signalId;

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
