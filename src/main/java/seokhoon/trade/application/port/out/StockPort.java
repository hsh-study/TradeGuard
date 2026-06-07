package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.stock.Stock;

import java.util.List;

public interface StockPort {
    Stock save(Stock stock);

    List<Stock> findAll();
}
