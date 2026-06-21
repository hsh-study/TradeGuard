package seokhoon.trade.application.service;

import org.springframework.stereotype.Service;
import seokhoon.trade.application.port.in.StreamStockOrderBookUseCase;
import seokhoon.trade.application.port.out.StreamingStockOrderBookPort;
import seokhoon.trade.application.port.out.StockOrderBookPort;
import java.util.function.Consumer;

@Service
public class StreamingStockOrderBookService implements StreamStockOrderBookUseCase {
    private final StreamingStockOrderBookPort port;

    public StreamingStockOrderBookService(StreamingStockOrderBookPort port) {
        this.port = port;
    }

    @Override
    public Subscription subscribe(String stockCode, long accountId,
            Consumer<StockOrderBookPort.Snapshot> consumer,
            Consumer<RuntimeException> errorConsumer) {
        var subscription = port.subscribe(stockCode, accountId, consumer, errorConsumer);
        return subscription::close;
    }
}
