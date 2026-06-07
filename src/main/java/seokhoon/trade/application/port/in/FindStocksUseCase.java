package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.stock.Stock;

import java.util.List;

public interface FindStocksUseCase {
    List<Stock> findAll();
}
