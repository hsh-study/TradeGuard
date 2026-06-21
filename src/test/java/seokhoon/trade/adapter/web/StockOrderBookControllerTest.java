package seokhoon.trade.adapter.web;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.GetStockOrderBookUseCase;
import seokhoon.trade.application.port.in.StreamStockOrderBookUseCase;
import seokhoon.trade.application.port.out.StockOrderBookPort;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class StockOrderBookControllerTest {
    @Test
    void returnsSelectedAccountOrderBook() throws Exception {
        GetStockOrderBookUseCase useCase = mock(GetStockOrderBookUseCase.class);
        when(useCase.get("005930", 2L)).thenReturn(new StockOrderBookPort.Snapshot(
                "005930", new BigDecimal("70000"),
                List.of(new StockOrderBookPort.Level(1, new BigDecimal("70100"), 12)),
                List.of(new StockOrderBookPort.Level(1, new BigDecimal("69900"), 15)),
                Instant.parse("2026-06-21T00:00:00Z")));

        standaloneSetup(new StockOrderBookController(useCase, mock(StreamStockOrderBookUseCase.class))).build()
                .perform(get("/api/stocks/orderbook")
                        .param("stockCode", "005930").param("accountId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asks[0].price").value(70100))
                .andExpect(jsonPath("$.bids[0].quantity").value(15));
    }

    @Test
    void opensServerSentEventRelayForKisWebSocket() throws Exception {
        GetStockOrderBookUseCase snapshots = mock(GetStockOrderBookUseCase.class);
        StreamStockOrderBookUseCase streams = mock(StreamStockOrderBookUseCase.class);
        when(streams.subscribe(eq("005930"), eq(2L), any(), any()))
                .thenReturn(() -> {});

        standaloneSetup(new StockOrderBookController(snapshots, streams)).build()
                .perform(get("/api/stocks/orderbook/stream")
                        .param("stockCode", "005930").param("accountId", "2"))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andExpect(content().contentType("text/event-stream"));

        verify(streams).subscribe(eq("005930"), eq(2L), any(), any());
        verifyNoInteractions(snapshots);
    }
}
