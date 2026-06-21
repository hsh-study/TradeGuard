package seokhoon.trade.application.port.in;

import seokhoon.trade.application.port.out.StockOrderBookPort;
import java.util.function.Consumer;

public interface StreamStockOrderBookUseCase {
    Subscription subscribe(String stockCode, long accountId,
            Consumer<StockOrderBookPort.Snapshot> consumer,
            Consumer<RuntimeException> errorConsumer);

    interface Subscription extends AutoCloseable {
        @Override void close();
    }
}
