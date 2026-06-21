package seokhoon.trade.application.service;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import seokhoon.trade.application.port.in.StreamStockChartUseCase;
import seokhoon.trade.application.port.out.MarketSnapshotPort;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

@Service
public class RealtimeStockChartService implements StreamStockChartUseCase {
    private static final ZoneId SEOUL=ZoneId.of("Asia/Seoul");
    private final MarketSnapshotPort snapshots; private final long intervalMs; private final int maxSymbols; private final Clock clock;
    private final ScheduledExecutorService scheduler; private final Map<String,Channel> channels=new ConcurrentHashMap<>();
    @Autowired
    public RealtimeStockChartService(MarketSnapshotPort snapshots,
            @Value("${tradeguard.market-data.chart-stream-interval-ms:5000}") long intervalMs,
            @Value("${tradeguard.market-data.chart-stream-max-symbols:3}") int maxSymbols){
        this(snapshots,intervalMs,maxSymbols,Clock.system(SEOUL));
    }
    RealtimeStockChartService(MarketSnapshotPort snapshots,long intervalMs,int maxSymbols,Clock clock){
        if(intervalMs<2000||maxSymbols<1)throw new IllegalArgumentException("invalid chart stream limits");
        this.snapshots=snapshots;this.intervalMs=intervalMs;this.maxSymbols=maxSymbols;this.clock=clock;
        scheduler=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"chart-stream");t.setDaemon(true);return t;});
    }
    @Override public Subscription subscribe(String stockCode,Consumer<LiveChartPoint> consumer){
        String code=requireCode(stockCode);Channel channel;
        synchronized(channels){channel=channels.get(code);if(channel==null){
            if(channels.size()>=maxSymbols)throw new IllegalStateException("chart stream symbol limit exceeded");
            channel=new Channel();channels.put(code,channel);Channel created=channel;
            created.future=scheduler.scheduleWithFixedDelay(()->poll(code),0,intervalMs,TimeUnit.MILLISECONDS);}
            channel.consumers.add(consumer);}
        Channel selected=channel;return ()->unsubscribe(code,selected,consumer);
    }
    private void poll(String code){ZonedDateTime now=ZonedDateTime.now(clock);if(!isMarketHours(now))return;try{snapshots.getSnapshot(code).ifPresent(s->{
        var point=new LiveChartPoint(code,now.toLocalDate(),now.toLocalTime(),s.currentPrice(),s.intradayHigh(),s.intradayLow(),s.currentPrice(),s.accumulatedVolume(),s.accumulatedTradingValue(),s.changeRate(),s.snapshotTime());
        Channel channel=channels.get(code);if(channel!=null)channel.consumers.forEach(c->{try{c.accept(point);}catch(RuntimeException ignored){}});
    });}catch(RuntimeException ignored){}}
    private void unsubscribe(String code,Channel channel,Consumer<LiveChartPoint> consumer){synchronized(channels){channel.consumers.remove(consumer);if(channel.consumers.isEmpty()&&channels.remove(code,channel))channel.future.cancel(false);}}
    static String requireCode(String value){if(value==null||!value.matches("[0-9A-Za-z]{1,12}"))throw new IllegalArgumentException("invalid stockCode");return value;}
    static boolean isMarketHours(ZonedDateTime now){DayOfWeek day=now.getDayOfWeek();LocalTime time=now.toLocalTime();return day!=DayOfWeek.SATURDAY&&day!=DayOfWeek.SUNDAY&&!time.isBefore(LocalTime.of(9,0))&&!time.isAfter(LocalTime.of(15,30));}
    @PreDestroy void close(){scheduler.shutdownNow();}
    private static final class Channel{final List<Consumer<LiveChartPoint>> consumers=new CopyOnWriteArrayList<>();ScheduledFuture<?> future;}
}
