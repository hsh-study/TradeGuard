package seokhoon.trade.adapter.web;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.FindStocksUseCase;
import seokhoon.trade.application.port.in.ManageStockUseCase;
import seokhoon.trade.application.port.in.RegisterStockUseCase;
import seokhoon.trade.domain.stock.Market;
import seokhoon.trade.domain.stock.Stock;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class StockControllerTest {
    @Test
    void removesStockFromWatchlistWithoutDeletingHistory() throws Exception {
        ManageStockUseCase manage = mock(ManageStockUseCase.class);
        when(manage.removeFromWatchlist("005930"))
                .thenReturn(new Stock("005930", "삼성전자", Market.KOSPI, false));
        var controller = new StockController(mock(RegisterStockUseCase.class),
                mock(FindStocksUseCase.class), manage);

        standaloneSetup(controller).build().perform(delete("/api/stocks/005930"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockCode").value("005930"))
                .andExpect(jsonPath("$.active").value(false));
        verify(manage).removeFromWatchlist("005930");
    }
}
