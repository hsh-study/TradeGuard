package seokhoon.trade.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface GetStockChartUseCase {
    StockChart getChart(String stockCode, LocalDate from, LocalDate to);
    default StockChart getChart(String stockCode, LocalDate from, LocalDate to,
            ChartInterval interval) {
        return getChart(stockCode, from, to);
    }

    enum ChartInterval {
        MINUTE_1(1), MINUTE_3(3), MINUTE_5(5), MINUTE_10(10),
        MINUTE_15(15), MINUTE_30(30), MINUTE_60(60), DAY(0), WEEK(0), MONTH(0), YEAR(0);
        private final int minutes;
        ChartInterval(int minutes) { this.minutes = minutes; }
        public int minutes() { return minutes; }
        public boolean intraday() { return minutes > 0; }
    }

    record StockChart(
            String stockCode,
            LocalDate from,
            LocalDate to,
            ChartInterval interval,
            int dataPointCount,
            List<ChartPoint> points
    ) {
        public StockChart(String stockCode, LocalDate from, LocalDate to,
                int dataPointCount, List<ChartPoint> points) {
            this(stockCode, from, to, ChartInterval.DAY, dataPointCount, points);
        }
    }

    record ChartPoint(
            LocalDate tradeDate,
            LocalTime barTime,
            BigDecimal open,
            BigDecimal high,
            BigDecimal low,
            BigDecimal close,
            long volume,
            BigDecimal tradingValue,
            BigDecimal ma5,
            BigDecimal ma20,
            BigDecimal ma60,
            BigDecimal rsi14,
            BigDecimal macd,
            BigDecimal macdSignal,
            BigDecimal macdHistogram,
            BigDecimal bollingerUpper,
            BigDecimal bollingerMiddle,
            BigDecimal bollingerLower
    ) {
        public ChartPoint(LocalDate tradeDate, BigDecimal open, BigDecimal high,
                BigDecimal low, BigDecimal close, long volume, BigDecimal tradingValue,
                BigDecimal ma5, BigDecimal ma20, BigDecimal ma60, BigDecimal rsi14,
                BigDecimal macd, BigDecimal macdSignal, BigDecimal macdHistogram,
                BigDecimal bollingerUpper, BigDecimal bollingerMiddle,
                BigDecimal bollingerLower) {
            this(tradeDate, null, open, high, low, close, volume, tradingValue,
                    ma5, ma20, ma60, rsi14, macd, macdSignal, macdHistogram,
                    bollingerUpper, bollingerMiddle, bollingerLower);
        }
    }
}
