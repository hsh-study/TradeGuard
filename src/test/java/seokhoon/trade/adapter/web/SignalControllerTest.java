package seokhoon.trade.adapter.web;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.MockOrderResult;
import seokhoon.trade.application.port.in.TradingSignalSearchCriteria;
import seokhoon.trade.application.port.in.TradingSignalView;
import seokhoon.trade.domain.order.OrderRequest;
import seokhoon.trade.domain.order.OrderSide;
import seokhoon.trade.domain.order.OrderStatus;
import seokhoon.trade.domain.order.OrderType;
import seokhoon.trade.domain.risk.RiskDecision;
import seokhoon.trade.domain.strategy.SignalType;
import seokhoon.trade.domain.strategy.TradingSignal;
import seokhoon.trade.domain.strategy.TradingSignalStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SignalControllerTest {
    private static final LocalDate SIGNAL_DATE = LocalDate.of(2026, 6, 5);

    @Test
    void returnsFilteredTradingSignalsWithSignalId() {
        var captured = new TradingSignalSearchCriteria[1];
        SignalController controller = new SignalController(
                criteria -> {
                    captured[0] = criteria;
                    return List.of(signalView());
                },
                (signalId, command) -> {
                    throw new UnsupportedOperationException();
                }
        );

        List<SignalController.TradingSignalResponse> response = controller.find(
                "005930",
                SIGNAL_DATE,
                "CLOSING_BET",
                SignalType.BUY_CANDIDATE,
                TradingSignalStatus.CREATED,
                70
        );

        assertThat(captured[0].stockCode()).isEqualTo("005930");
        assertThat(captured[0].signalDate()).isEqualTo(SIGNAL_DATE);
        assertThat(captured[0].strategyName()).isEqualTo("CLOSING_BET");
        assertThat(captured[0].signalType()).isEqualTo(SignalType.BUY_CANDIDATE);
        assertThat(captured[0].status()).isEqualTo(TradingSignalStatus.CREATED);
        assertThat(captured[0].minScore()).isEqualTo(70);
        assertThat(response)
                .singleElement()
                .satisfies(signal -> {
                    assertThat(signal.signalId()).isEqualTo(1L);
                    assertThat(signal.score()).isEqualTo(80);
                    assertThat(signal.reasons()).containsExactly("TEST");
                });
    }

    @Test
    void requestsMockOrderBySignalId() {
        SignalController controller = new SignalController(
                criteria -> List.of(),
                (signalId, command) -> {
                    assertThat(signalId).isEqualTo(1L);
                    assertThat(command.quantity()).isEqualTo(1);
                    assertThat(command.limitPrice()).isEqualByComparingTo("50000");
                    TradingSignal signal = signal();
                    signal.approveRisk();
                    signal.markOrderRequested();
                    return MockOrderResult.accepted(RiskDecision.approve(), signal, acceptedOrder());
                }
        );

        MockOrderController.MockOrderResponse response = controller.requestMockOrder(
                1L,
                new SignalController.SignalMockOrderRequest(1, BigDecimal.valueOf(50_000))
        );

        assertThat(response.approved()).isTrue();
        assertThat(response.signalStatus()).isEqualTo(TradingSignalStatus.ORDER_REQUESTED);
        assertThat(response.order().status()).isEqualTo(OrderStatus.ACCEPTED);
    }

    private static TradingSignalView signalView() {
        return new TradingSignalView(
                1L,
                "CLOSING_BET",
                "005930",
                SIGNAL_DATE,
                SignalType.BUY_CANDIDATE,
                80,
                List.of("TEST"),
                List.of(),
                TradingSignalStatus.CREATED
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

    private static OrderRequest acceptedOrder() {
        OrderRequest orderRequest = new OrderRequest(
                "005930",
                OrderSide.BUY,
                OrderType.LIMIT,
                1,
                BigDecimal.valueOf(50_000),
                "CLOSING_BET",
                SIGNAL_DATE
        );
        orderRequest.markRequested();
        orderRequest.accept("FAKE-ORDER");
        return orderRequest;
    }
}
