package seokhoon.trade.adapter.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import seokhoon.trade.application.port.in.GetStockChartUseCase;
import seokhoon.trade.application.port.in.GetStockChartUseCase.ChartInterval;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/stocks/chart")
public class StockChartController {
    private final GetStockChartUseCase useCase;

    public StockChartController(GetStockChartUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    GetStockChartUseCase.StockChart chart(
            @RequestParam String stockCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "DAY") ChartInterval interval
    ) {
        return useCase.getChart(stockCode, from, to, interval);
    }
}
