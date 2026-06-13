package seokhoon.trade.adapter.web;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import seokhoon.trade.application.port.in.*;
import seokhoon.trade.domain.order.*;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LiveTradingControllerTest {
    @Test
    void exposesManualBuyAndExitPreview() throws Exception {
        var buy=mock(LiveTradingUseCases.RequestLiveBuyUseCase.class);
        var sell=mock(LiveTradingUseCases.RequestLiveSellUseCase.class);
        var preview=mock(LiveTradingUseCases.PreviewLivePositionExitUseCase.class);
        var load=mock(LiveTradingUseCases.LoadLiveTradingUseCase.class);
        var kill=mock(LiveTradingUseCases.SetLiveTradingKillSwitchUseCase.class);
        var cancel=mock(LiveTradingUseCases.CancelLiveOrderUseCase.class);
        when(buy.buy(eq(7L),eq("005930"),eq(1),any(),eq(OrderType.LIMIT)))
                .thenReturn(order());
        when(preview.preview(eq(3L),any())).thenReturn(new LivePositionExitPreview(
                amount(100),amount(105),BigDecimal.ZERO,BigDecimal.ZERO,
                BigDecimal.ZERO,amount(5),amount(5),true,false,false,
                LiveExitAction.SELL_TAKE_PROFIT));
        MockMvc mvc=MockMvcBuilders.standaloneSetup(
                new LiveTradingController(buy,sell,preview,load,kill,cancel)).build();

        mvc.perform(post("/api/live-orders/buy")
                        .contentType("application/json")
                        .content("""
                                {"signalId":7,"stockCode":"005930","quantity":1,
                                 "orderPrice":70000,"orderType":"LIMIT"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
        mvc.perform(get("/api/live-positions/3/exit-preview")
                        .param("currentPrice","105"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestedAction")
                        .value("SELL_TAKE_PROFIT"));
    }

    @Test
    void rejectsMarketOrderAtValidationBoundary() throws Exception {
        var buy=mock(LiveTradingUseCases.RequestLiveBuyUseCase.class);
        MockMvc mvc=MockMvcBuilders.standaloneSetup(new LiveTradingController(
                buy,mock(LiveTradingUseCases.RequestLiveSellUseCase.class),
                mock(LiveTradingUseCases.PreviewLivePositionExitUseCase.class),
                mock(LiveTradingUseCases.LoadLiveTradingUseCase.class),
                mock(LiveTradingUseCases.SetLiveTradingKillSwitchUseCase.class),
                mock(LiveTradingUseCases.CancelLiveOrderUseCase.class)))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        when(buy.buy(any(),any(),anyInt(),any(),isNull()))
                .thenThrow(new IllegalArgumentException("Only LIMIT orders are allowed"));

        mvc.perform(post("/api/live-orders/buy")
                        .contentType("application/json")
                        .content("""
                                {"stockCode":"005930","quantity":1,
                                 "orderPrice":70000,"orderType":null}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exposesCancelAndOpenOrderQueries() throws Exception {
        var cancel=mock(LiveTradingUseCases.CancelLiveOrderUseCase.class);
        var load=mock(LiveTradingUseCases.LoadLiveTradingUseCase.class);
        when(load.openOrders()).thenReturn(java.util.List.of(order()));
        when(cancel.cancel(1L,null,"operator")).thenReturn(
                new LiveTradingUseCases.LiveOrderCancelResult(
                        order().withCanceled(Instant.now()),
                        new LiveOrderCancelRequest(1L,1L,null,1,
                                LiveOrderCancelStatus.ACCEPTED,"CANCEL",null,
                                "operator",Instant.now(),Instant.now(),
                                Instant.now())));
        MockMvc mvc=MockMvcBuilders.standaloneSetup(new LiveTradingController(
                mock(LiveTradingUseCases.RequestLiveBuyUseCase.class),
                mock(LiveTradingUseCases.RequestLiveSellUseCase.class),
                mock(LiveTradingUseCases.PreviewLivePositionExitUseCase.class),
                load,
                mock(LiveTradingUseCases.SetLiveTradingKillSwitchUseCase.class),
                cancel)).build();

        mvc.perform(get("/api/live-orders/open"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ACCEPTED"));
        mvc.perform(post("/api/live-orders/1/cancel")
                        .contentType("application/json")
                        .content("""
                                {"reason":"operator"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order.status").value("CANCELED"));
    }

    private static LiveOrderRequest order(){Instant now=Instant.now();return new LiveOrderRequest(1L,7L,"005930",OrderSide.BUY,1,amount(70000),OrderType.LIMIT,LiveOrderStatus.ACCEPTED,"ORDER",null,null,now,now,now);}
    private static BigDecimal amount(long value){return BigDecimal.valueOf(value);}
}
