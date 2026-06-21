package seokhoon.trade.application.service;

import org.springframework.stereotype.Service;
import seokhoon.trade.application.port.in.GetStockOrderBookUseCase;
import seokhoon.trade.application.port.out.StockOrderBookPort;

@Service
public class StockOrderBookService implements GetStockOrderBookUseCase {
    private final StockOrderBookPort port;

    public StockOrderBookService(StockOrderBookPort port) { this.port = port; }

    @Override
    public StockOrderBookPort.Snapshot get(String stockCode, long accountId) {
        if (stockCode == null || !stockCode.matches("[0-9A-Za-z]{1,12}")) {
            throw new IllegalArgumentException("invalid stockCode");
        }
        if (accountId < 1) throw new IllegalArgumentException("invalid accountId");
        return port.load(stockCode, accountId);
    }
}
