package seokhoon.trade.adapter.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import seokhoon.trade.application.port.in.GetStockOrderBookUseCase;
import seokhoon.trade.application.port.in.StreamStockOrderBookUseCase;
import seokhoon.trade.application.port.out.StockOrderBookPort;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("/api/stocks/orderbook")
public class StockOrderBookController {
    private final GetStockOrderBookUseCase useCase;
    private final StreamStockOrderBookUseCase streamUseCase;
    public StockOrderBookController(GetStockOrderBookUseCase useCase,
            StreamStockOrderBookUseCase streamUseCase) {
        this.useCase = useCase;
        this.streamUseCase = streamUseCase;
    }

    @GetMapping
    StockOrderBookPort.Snapshot get(@RequestParam String stockCode,
            @RequestParam long accountId) {
        return useCase.get(stockCode, accountId);
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter stream(@RequestParam String stockCode, @RequestParam long accountId)
            throws IOException {
        SseEmitter emitter = new SseEmitter(0L);
        AtomicBoolean closed = new AtomicBoolean();
        var subscription = streamUseCase.subscribe(stockCode, accountId, snapshot -> {
            try {
                emitter.send(SseEmitter.event().name("orderbook").data(snapshot));
            } catch (IOException | IllegalStateException exception) {
                emitter.complete();
            }
        }, error -> emitter.completeWithError(error));
        Runnable close = () -> {
            if (closed.compareAndSet(false, true)) subscription.close();
        };
        emitter.onCompletion(close);
        emitter.onTimeout(close);
        emitter.onError(error -> close.run());
        emitter.send(SseEmitter.event().name("connected").data("kis-websocket"));
        return emitter;
    }
}
