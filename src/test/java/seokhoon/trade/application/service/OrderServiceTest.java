package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.MockOrderResult;
import seokhoon.trade.application.port.out.BrokerPort;
import seokhoon.trade.application.port.out.OrderRequestPort;
import seokhoon.trade.application.port.out.TradingSignalPort;
import seokhoon.trade.domain.order.OrderRequest;
import seokhoon.trade.domain.order.OrderSide;
import seokhoon.trade.domain.risk.RiskManager;
import seokhoon.trade.domain.strategy.SignalType;
import seokhoon.trade.domain.strategy.TradingSignal;
import seokhoon.trade.domain.strategy.TradingSignalStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
        assertThat(orderPort.saveCalls).isZero();
    }

    @Test
    void persistsAcceptedOrderAndMarksSignalAsOrderRequested() {
        RecordingOrderRequestPort orderPort = new RecordingOrderRequestPort(false);
        RecordingTradingSignalPort signalPort = new RecordingTradingSignalPort();
        RecordingBrokerPort brokerPort = new RecordingBrokerPort();
        OrderService service = new OrderService(orderPort, signalPort, brokerPort, new RiskManager());

        MockOrderResult result = service.request(signal(80), 1, BigDecimal.valueOf(50_000));

        assertThat(result.riskDecision().approved()).isTrue();
        assertThat(result.orderRequest()).isNotNull();
        assertThat(result.tradingSignal().status()).isEqualTo(TradingSignalStatus.ORDER_REQUESTED);
        assertThat(signalPort.savedStatuses)
                .containsExactly(TradingSignalStatus.RISK_APPROVED, TradingSignalStatus.ORDER_REQUESTED);
        assertThat(brokerPort.calls).isEqualTo(1);
        assertThat(orderPort.saveCalls).isEqualTo(1);
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
        private int saveCalls;

        private RecordingOrderRequestPort(boolean existing) {
            this.existing = existing;
        }

        @Override
        public OrderRequest save(OrderRequest orderRequest) {
            saveCalls++;
            return orderRequest;
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
    }

    private static class RecordingTradingSignalPort implements TradingSignalPort {
        private final List<TradingSignalStatus> savedStatuses = new ArrayList<>();

        @Override
        public TradingSignal save(TradingSignal tradingSignal) {
            savedStatuses.add(tradingSignal.status());
            return tradingSignal;
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
}
