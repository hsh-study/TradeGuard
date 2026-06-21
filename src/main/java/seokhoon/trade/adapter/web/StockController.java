package seokhoon.trade.adapter.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import seokhoon.trade.application.port.in.FindStocksUseCase;
import seokhoon.trade.application.port.in.RegisterStockUseCase;
import seokhoon.trade.application.port.in.ManageStockUseCase;
import seokhoon.trade.domain.stock.Market;
import seokhoon.trade.domain.stock.Stock;
import seokhoon.trade.domain.indicator.IndicatorWarmUpResult;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
public class StockController {
    private final RegisterStockUseCase registerStockUseCase;
    private final FindStocksUseCase findStocksUseCase;
    private final ManageStockUseCase manageStockUseCase;

    public StockController(RegisterStockUseCase registerStockUseCase, FindStocksUseCase findStocksUseCase,
                           ManageStockUseCase manageStockUseCase) {
        this.registerStockUseCase = registerStockUseCase;
        this.findStocksUseCase = findStocksUseCase;
        this.manageStockUseCase = manageStockUseCase;
    }

    @PostMapping
    RegisterStockResponse register(
            @RequestBody RegisterStockRequest request
    ) {
        IndicatorWarmUpResult warmUp = registerStockUseCase.register(
                request.stockCode(),
                request.stockName(),
                request.market()
        );
        return new RegisterStockResponse(
                request.stockCode(),
                true,
                IndicatorWarmUpController.WarmUpResponse.from(warmUp)
        );
    }

    @GetMapping
    List<StockResponse> findAll() {
        return findStocksUseCase.findAll().stream()
                .map(StockResponse::from)
                .toList();
    }

    @PostMapping("/{stockCode}/active")
    StockResponse changeActive(@PathVariable String stockCode, @RequestParam boolean value) {
        return StockResponse.from(manageStockUseCase.changeActive(stockCode, value));
    }

    @DeleteMapping("/{stockCode}")
    StockResponse remove(@PathVariable String stockCode) {
        return StockResponse.from(manageStockUseCase.removeFromWatchlist(stockCode));
    }

    public record RegisterStockRequest(String stockCode, String stockName, Market market) {
    }

    public record RegisterStockResponse(
            String stockCode,
            boolean registered,
            IndicatorWarmUpController.WarmUpResponse warmUp
    ) {
    }

    public record StockResponse(String stockCode, String stockName, Market market, boolean active) {
        static StockResponse from(Stock stock) {
            return new StockResponse(stock.stockCode(), stock.stockName(), stock.market(), stock.active());
        }
    }
}
