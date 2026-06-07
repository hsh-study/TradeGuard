package seokhoon.trade.application.service;

import org.springframework.stereotype.Service;
import seokhoon.trade.application.port.in.FindStocksUseCase;
import seokhoon.trade.application.port.in.RegisterStockUseCase;
import seokhoon.trade.application.port.out.StockPort;
import seokhoon.trade.domain.stock.Market;
import seokhoon.trade.domain.stock.Stock;

import java.util.List;

@Service
public class StockService implements RegisterStockUseCase, FindStocksUseCase {
    private final StockPort stockPort;

    public StockService(StockPort stockPort) {
        this.stockPort = stockPort;
    }

    @Override
    public void register(String stockCode, String stockName, Market market) {
        stockPort.save(new Stock(stockCode, stockName, market, true));
    }

    @Override
    public List<Stock> findAll() {
        return stockPort.findAll();
    }
}
