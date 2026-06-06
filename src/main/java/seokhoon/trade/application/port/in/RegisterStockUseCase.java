package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.stock.Market;

public interface RegisterStockUseCase {
    void register(String stockCode, String stockName, Market market);
}
