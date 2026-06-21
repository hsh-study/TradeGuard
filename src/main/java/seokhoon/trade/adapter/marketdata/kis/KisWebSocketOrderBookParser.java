package seokhoon.trade.adapter.marketdata.kis;

import seokhoon.trade.application.port.out.StockOrderBookPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

final class KisWebSocketOrderBookParser {
    private KisWebSocketOrderBookParser() {}

    static StockOrderBookPort.Snapshot orderBook(String payload,
            BigDecimal executionPrice, Instant receivedAt) {
        String[] fields = payload.split("\\^", -1);
        if (fields.length < 43 || fields[0].isBlank()) return null;
        List<StockOrderBookPort.Level> asks = new ArrayList<>();
        List<StockOrderBookPort.Level> bids = new ArrayList<>();
        for (int level = 1; level <= 10; level++) {
            BigDecimal ask = decimal(fields, level + 2);
            BigDecimal bid = decimal(fields, level + 12);
            long askQuantity = integer(fields, level + 22);
            long bidQuantity = integer(fields, level + 32);
            if (ask != null && ask.signum() > 0) asks.add(new StockOrderBookPort.Level(level, ask, askQuantity));
            if (bid != null && bid.signum() > 0) bids.add(new StockOrderBookPort.Level(level, bid, bidQuantity));
        }
        BigDecimal anticipatedPrice = decimal(fields, 47);
        BigDecimal current = executionPrice == null ? anticipatedPrice : executionPrice;
        return new StockOrderBookPort.Snapshot(fields[0], current,
                List.copyOf(asks), List.copyOf(bids), receivedAt);
    }

    static BigDecimal executionPrice(String payload) {
        return decimal(payload.split("\\^", -1), 2);
    }

    private static BigDecimal decimal(String[] fields, int index) {
        if (index >= fields.length || fields[index].isBlank()) return null;
        try { return new BigDecimal(fields[index]); }
        catch (NumberFormatException ignored) { return null; }
    }

    private static long integer(String[] fields, int index) {
        if (index >= fields.length || fields[index].isBlank()) return 0;
        try { return Long.parseLong(fields[index]); }
        catch (NumberFormatException ignored) { return 0; }
    }
}
