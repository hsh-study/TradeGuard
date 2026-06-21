package seokhoon.trade.adapter.web;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import seokhoon.trade.application.port.in.StreamStockChartUseCase;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
@RestController @RequestMapping("/api/stocks/chart")
public class RealtimeStockChartController {
    private final StreamStockChartUseCase useCase;
    public RealtimeStockChartController(StreamStockChartUseCase useCase){this.useCase=useCase;}
    @GetMapping(path="/stream",produces=MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter stream(@RequestParam String stockCode)throws IOException{
        SseEmitter emitter=new SseEmitter(0L);AtomicBoolean closed=new AtomicBoolean();
        var subscription=useCase.subscribe(stockCode,point->{try{emitter.send(SseEmitter.event().name("quote").data(point));}catch(IOException|IllegalStateException e){emitter.complete();}});
        Runnable close=()->{if(closed.compareAndSet(false,true))subscription.close();};
        emitter.onCompletion(close);emitter.onTimeout(close);emitter.onError(e->close.run());
        emitter.send(SseEmitter.event().name("connected").data("read-only"));return emitter;
    }
}
