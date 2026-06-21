package seokhoon.trade.adapter.web;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.GetWatchlistPortfolioUseCase;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class WatchlistPortfolioControllerTest {
    @Test
    void exposesWatchlistAndEnvironmentSeparatedHoldings() throws Exception {
        GetWatchlistPortfolioUseCase useCase = mock(GetWatchlistPortfolioUseCase.class);
        when(useCase.watchlist()).thenReturn(List.of());
        when(useCase.holdings()).thenReturn(List.of());
        when(useCase.holdings(2L)).thenReturn(List.of());
        var mvc = standaloneSetup(new WatchlistPortfolioController(useCase)).build();

        mvc.perform(get("/api/operations/portfolio/watchlist"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/api/operations/portfolio/holdings"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/api/operations/portfolio/holdings").param("accountId", "2"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(useCase).holdings(2L);
    }
}
