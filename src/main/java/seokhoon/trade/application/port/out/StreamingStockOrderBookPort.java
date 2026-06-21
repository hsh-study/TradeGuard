package seokhoon.trade.application.port.out;

import java.util.function.Consumer;

public interface StreamingStockOrderBookPort {
    Subscription subscribe(String stockCode, long accountId,
            Consumer<StockOrderBookPort.Snapshot> consumer,
            Consumer<RuntimeException> errorConsumer);

    interface Subscription extends AutoCloseable {
        @Override void close();
    }
}
