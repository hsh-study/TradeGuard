package seokhoon.trade.application.port.in;
import java.math.BigDecimal;
import java.time.*;
import java.util.function.Consumer;
public interface StreamStockChartUseCase {
    Subscription subscribe(String stockCode, Consumer<LiveChartPoint> consumer);
    interface Subscription extends AutoCloseable { @Override void close(); }
    record LiveChartPoint(String stockCode, LocalDate tradeDate, LocalTime tradeTime,
            BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close,
            long cumulativeVolume, BigDecimal cumulativeTradingValue,
            BigDecimal changeRate, Instant receivedAt) {}
}
