package seokhoon.trade.application.port.out;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface StockOrderBookPort {
    Snapshot load(String stockCode, long accountId);

    record Level(int level, BigDecimal price, long quantity) {}
    record Snapshot(String stockCode, BigDecimal currentPrice,
            List<Level> asks, List<Level> bids, Instant generatedAt) {}
}
