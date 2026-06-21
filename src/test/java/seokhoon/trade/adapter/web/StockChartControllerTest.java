package seokhoon.trade.adapter.web;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.GetStockChartUseCase;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class StockChartControllerTest {
    @Test
    void returnsDatabaseChartDataForRequestedRange() throws Exception {
        GetStockChartUseCase useCase = mock(GetStockChartUseCase.class);
        LocalDate from = LocalDate.of(2026, 1, 1), to = LocalDate.of(2026, 6, 20);
        when(useCase.getChart("005930", from, to, GetStockChartUseCase.ChartInterval.DAY))
                .thenReturn(new GetStockChartUseCase.StockChart("005930", from, to, 0, List.of()));

        standaloneSetup(new StockChartController(useCase)).build()
                .perform(get("/api/stocks/chart").param("stockCode", "005930")
                        .param("from", from.toString()).param("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockCode").value("005930"))
                .andExpect(jsonPath("$.dataPointCount").value(0));
        verify(useCase).getChart("005930", from, to, GetStockChartUseCase.ChartInterval.DAY);
    }

    @Test
    void acceptsMinuteAndLongTermIntervals() throws Exception {
        GetStockChartUseCase useCase = mock(GetStockChartUseCase.class);
        LocalDate date = LocalDate.of(2026, 6, 22);
        when(useCase.getChart("005930", date, date, GetStockChartUseCase.ChartInterval.MINUTE_3))
                .thenReturn(new GetStockChartUseCase.StockChart("005930", date, date,
                        GetStockChartUseCase.ChartInterval.MINUTE_3, 0, List.of()));
        standaloneSetup(new StockChartController(useCase)).build()
                .perform(get("/api/stocks/chart").param("stockCode", "005930")
                        .param("from", date.toString()).param("to", date.toString())
                        .param("interval", "MINUTE_3"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.interval").value("MINUTE_3"));
    }
}
