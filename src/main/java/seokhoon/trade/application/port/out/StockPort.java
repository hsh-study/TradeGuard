package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.stock.Stock;

import java.util.List;
import java.util.Optional;

public interface StockPort {
    Stock save(Stock stock);

    List<Stock> findAll();

    default Optional<Stock> findByStockCode(String stockCode) {
        return findAll().stream().filter(stock -> stock.stockCode().equals(stockCode)).findFirst();
    }
}
