package seokhoon.trade.adapter.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import seokhoon.trade.application.port.in.FindStocksUseCase;
import seokhoon.trade.application.port.in.RegisterStockUseCase;
import seokhoon.trade.domain.stock.Market;
import seokhoon.trade.domain.stock.Stock;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
public class StockController {
    private final RegisterStockUseCase registerStockUseCase;
    private final FindStocksUseCase findStocksUseCase;

    public StockController(RegisterStockUseCase registerStockUseCase, FindStocksUseCase findStocksUseCase) {
        this.registerStockUseCase = registerStockUseCase;
        this.findStocksUseCase = findStocksUseCase;
    }

    @PostMapping
    void register(@RequestBody RegisterStockRequest request) {
        registerStockUseCase.register(request.stockCode(), request.stockName(), request.market());
    }

    @GetMapping
    List<StockResponse> findAll() {
        return findStocksUseCase.findAll().stream()
                .map(StockResponse::from)
                .toList();
    }

    public record RegisterStockRequest(String stockCode, String stockName, Market market) {
    }

    public record StockResponse(String stockCode, String stockName, Market market, boolean active) {
        static StockResponse from(Stock stock) {
            return new StockResponse(stock.stockCode(), stock.stockName(), stock.market(), stock.active());
        }
    }
}
