package seokhoon.trade.adapter.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import seokhoon.trade.adapter.persistence.StockEntity;
import seokhoon.trade.application.service.StockService;
import seokhoon.trade.domain.stock.Market;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
public class StockController {
    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @PostMapping
    void register(@RequestBody RegisterStockRequest request) {
        stockService.register(request.stockCode(), request.stockName(), request.market());
    }

    @GetMapping
    List<StockEntity> findAll() {
        return stockService.findAll();
    }

    public record RegisterStockRequest(String stockCode, String stockName, Market market) {
    }
}
