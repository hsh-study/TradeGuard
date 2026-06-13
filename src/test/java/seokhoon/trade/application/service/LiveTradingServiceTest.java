package seokhoon.trade.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.*;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.config.*;
import seokhoon.trade.domain.order.*;
import seokhoon.trade.domain.position.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LiveTradingServiceTest {
    private static final Instant NOW=Instant.parse("2026-06-12T01:00:00Z");
    private LiveTradingProperties properties;
    private LiveOrderRequestPort orders;
    private LivePositionPort positions;
    private LivePositionExitRulePort rules;
    private LiveTradeFillPort fills;
    private LiveOrderStatusHistoryPort histories;
    private LiveTradingRuntimeStatePort runtime;
    private LiveTradingOrderPort broker;
    private LiveTradingService service;

    @BeforeEach
    void setUp(){
        properties=new LiveTradingProperties();
        properties.setAccountNumber("ACCOUNT");
        properties.setAccountProductCode("01");
        orders=mock(LiveOrderRequestPort.class);
        positions=mock(LivePositionPort.class);
        rules=mock(LivePositionExitRulePort.class);
        fills=mock(LiveTradeFillPort.class);
        histories=mock(LiveOrderStatusHistoryPort.class);
        runtime=mock(LiveTradingRuntimeStatePort.class);
        broker=mock(LiveTradingOrderPort.class);
        when(runtime.get()).thenReturn(new LiveTradingRuntimeState(false,null,NOW));
        when(orders.save(any())).thenAnswer(invocation->withOrderId(invocation.getArgument(0)));
        when(broker.submitBuyLimitOrder(any())).thenReturn(LiveOrderSubmission.accepted("ORDER",null));
        when(broker.submitSellLimitOrder(any())).thenReturn(LiveOrderSubmission.accepted("ORDER",null));
        service=new LiveTradingService(properties,orders,positions,rules,fills,
                histories,runtime,broker,code->BigDecimal.valueOf(100),
                date->true,OperationalMetricsPort.noop(),
                Clock.fixed(NOW,ZoneId.of("Asia/Seoul")));
    }

    @Test void rejectsWhenLiveTradingDisabled(){
        properties.setKisTradingEnabled(true);
        assertThatThrownBy(()->service.buy(null,"005930",1,money(100),OrderType.LIMIT))
                .isInstanceOf(LiveTradingDisabledException.class);
        verifyNoInteractions(broker);
    }

    @Test void rejectsWhenKisTradingDisabled(){
        properties.setLiveTradingEnabled(true);
        assertThatThrownBy(()->service.buy(null,"005930",1,money(100),OrderType.LIMIT))
                .isInstanceOf(LiveTradingDisabledException.class);
    }

    @Test void rejectsNonLimitAndExcessAmount(){
        enable();
        assertThatThrownBy(()->service.buy(null,"005930",1,money(100),null))
                .isInstanceOf(LiveTradingException.class)
                .hasMessageContaining("LIMIT");
        assertThatThrownBy(()->service.buy(null,"005930",2,money(600000),OrderType.LIMIT))
                .isInstanceOf(LiveTradingException.class)
                .hasMessageContaining("maxAllowedOrderAmount");
    }

    @Test void rejectsDuplicateSignalBuy(){
        enable();
        when(orders.existsBySignalIdAndSide(7L,OrderSide.BUY)).thenReturn(true);
        assertThatThrownBy(()->service.buy(7L,"005930",1,money(100),OrderType.LIMIT))
                .isInstanceOf(LiveTradingException.class)
                .hasMessageContaining("already exists");
    }

    @Test void savesAcceptedBuyOrder(){
        enable();
        LiveOrderRequest result=service.buy(7L,"005930",1,money(100),OrderType.LIMIT);
        assertThat(result.status()).isEqualTo(LiveOrderStatus.ACCEPTED);
        assertThat(result.kisOrderNo()).isEqualTo("ORDER");
        verify(broker).submitBuyLimitOrder(any());
    }

    @Test void blocksNewOrderWhenKillSwitchEnabled(){
        enable();
        when(runtime.get()).thenReturn(new LiveTradingRuntimeState(true,"operator",NOW));
        assertThatThrownBy(()->service.buy(null,"005930",1,money(100),OrderType.LIMIT))
                .isInstanceOf(LiveTradingException.class)
                .hasMessageContaining("kill switch");
    }

    @Test void createsPositionOnlyAfterBuyFill(){
        LiveOrderRequest accepted=acceptedOrder(1L,OrderSide.BUY);
        when(orders.findOrderById(1L)).thenReturn(Optional.of(accepted));
        when(positions.savePosition(any())).thenAnswer(invocation->{
            LivePosition p=invocation.getArgument(0);
            return new LivePosition(3L,p.stockCode(),p.quantity(),p.averageBuyPrice(),
                    p.buyAmount(),p.buyCommission(),p.status(),p.openedAt(),null);
        });
        when(rules.save(any())).thenAnswer(invocation->invocation.getArgument(0));
        LivePosition result=service.apply(new LiveTradeFill(null,1L,"005930",
                OrderSide.BUY,2,money(100),money(200),money(1),BigDecimal.ZERO,NOW));
        assertThat(result.status()).isEqualTo(LivePositionStatus.OPEN);
        verify(positions).savePosition(any());
        verify(rules).save(any());
    }

    @Test void grossFivePercentDoesNotTriggerTakeProfitWhenNetReturnIsBelowFive(){
        LivePosition position=position(1L,money(10000),100);
        stubPosition(position);
        LivePositionExitPreview preview=service.preview(1L,money(10500));
        assertThat(preview.estimatedNetReturnRate()).isLessThan(new BigDecimal("5.0"));
        assertThat(preview.takeProfitTriggered()).isFalse();
        assertThat(preview.suggestedAction()).isEqualTo(LiveExitAction.HOLD);
    }

    @Test void triggersStopLossAndMaxLossBeforeRateRule(){
        LivePosition position=position(1L,money(10000),100);
        stubPosition(position);
        LivePositionExitPreview stop=service.preview(1L,money(9700));
        assertThat(stop.stopLossTriggered()).isTrue();
        assertThat(stop.suggestedAction()).isEqualTo(LiveExitAction.SELL_STOP_LOSS);

        properties.setMaxLossAmountPerPosition(money(1000));
        LivePositionExitPreview maxLoss=service.preview(1L,money(9850));
        assertThat(maxLoss.maxLossTriggered()).isTrue();
        assertThat(maxLoss.stopLossTriggered()).isFalse();
    }

    @Test void usesReducedThresholdForHighPricedStock(){
        LivePosition position=position(1L,money(250000),1);
        stubPosition(position);
        LivePositionExitPreview preview=service.preview(1L,money(259000));
        assertThat(preview.estimatedNetReturnRate()).isGreaterThan(new BigDecimal("3.0"));
        assertThat(preview.takeProfitTriggered()).isTrue();
    }

    @Test void rejectsDuplicateSellForSellOrderedPosition(){
        enable();
        LivePosition p=new LivePosition(1L,"005930",1,money(100),money(100),
                BigDecimal.ZERO,LivePositionStatus.SELL_ORDERED,NOW,null);
        when(positions.findPositionById(1L)).thenReturn(Optional.of(p));
        assertThatThrownBy(()->service.sell(1L,null,1,money(100),"manual"))
                .isInstanceOf(LiveTradingException.class)
                .hasMessageContaining("not OPEN");
    }

    private void enable(){properties.setLiveTradingEnabled(true);properties.setKisTradingEnabled(true);}
    private void stubPosition(LivePosition p){when(positions.findPositionById(p.id())).thenReturn(Optional.of(p));when(rules.findByPositionId(p.id())).thenReturn(Optional.empty());}
    private static LivePosition position(long id,BigDecimal price,int quantity){return new LivePosition(id,"005930",quantity,price,price.multiply(BigDecimal.valueOf(quantity)),BigDecimal.ZERO,LivePositionStatus.OPEN,NOW,null);}
    private static LiveOrderRequest acceptedOrder(long id,OrderSide side){return new LiveOrderRequest(id,null,"005930",side,2,money(100),OrderType.LIMIT,LiveOrderStatus.ACCEPTED,"ORDER",null,null,NOW,NOW,NOW);}
    private static BigDecimal money(long value){return BigDecimal.valueOf(value);}
    private static LiveOrderRequest withOrderId(LiveOrderRequest o){return new LiveOrderRequest(o.id()==null?1L:o.id(),o.signalId(),o.stockCode(),o.side(),o.quantity(),o.orderPrice(),o.orderType(),o.status(),o.kisOrderNo(),o.kisOriginalOrderNo(),o.failureReason(),o.requestedAt(),o.submittedAt(),o.updatedAt());}
}
