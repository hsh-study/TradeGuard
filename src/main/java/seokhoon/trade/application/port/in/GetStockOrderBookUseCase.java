package seokhoon.trade.application.port.in;

import seokhoon.trade.application.port.out.StockOrderBookPort;

public interface GetStockOrderBookUseCase {
    StockOrderBookPort.Snapshot get(String stockCode, long accountId);
}
