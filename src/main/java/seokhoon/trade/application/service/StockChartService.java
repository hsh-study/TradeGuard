package seokhoon.trade.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import seokhoon.trade.application.port.in.GetStockChartUseCase;
import seokhoon.trade.application.port.out.DailyPricePort;
import seokhoon.trade.application.port.out.IntradayBarPort;
import seokhoon.trade.domain.indicator.BollingerBand;
import seokhoon.trade.domain.indicator.Macd;
import seokhoon.trade.domain.indicator.TechnicalIndicatorCalculator;
import seokhoon.trade.domain.market.BarInterval;
import seokhoon.trade.domain.market.DailyPrice;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.WeekFields;
import java.util.*;

@Service
public class StockChartService implements GetStockChartUseCase {
    private static final int MAX_CALENDAR_DAYS = 3_650;
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private final DailyPricePort dailyPrices;
    private final IntradayBarPort intradayBars;
    private final TechnicalIndicatorCalculator calculator;
    private final Clock clock;

    @Autowired
    public StockChartService(DailyPricePort dailyPrices, IntradayBarPort intradayBars,
            TechnicalIndicatorCalculator calculator) {
        this(dailyPrices, intradayBars, calculator, Clock.system(SEOUL));
    }

    StockChartService(DailyPricePort dailyPrices, TechnicalIndicatorCalculator calculator) {
        this(dailyPrices, null, calculator, Clock.system(SEOUL));
    }

    StockChartService(DailyPricePort dailyPrices, IntradayBarPort intradayBars,
            TechnicalIndicatorCalculator calculator, Clock clock) {
        this.dailyPrices = dailyPrices;
        this.intradayBars = intradayBars;
        this.calculator = calculator;
        this.clock = clock;
    }

    @Override
    public StockChart getChart(String stockCode, LocalDate from, LocalDate to) {
        return getChart(stockCode, from, to, ChartInterval.DAY);
    }

    @Override
    public StockChart getChart(String stockCode, LocalDate from, LocalDate to,
            ChartInterval interval) {
        String code = requireStockCode(stockCode);
        validateRange(from, to);
        Objects.requireNonNull(interval, "interval");
        List<Candle> candles = interval.intraday()
                ? intraday(code, from, to, interval.minutes())
                : daily(code, from, to, interval);
        List<ChartPoint> points = indicators(code, candles);
        return new StockChart(code, from, to, interval, points.size(), points);
    }

    private List<Candle> intraday(String code, LocalDate from, LocalDate to, int minutes) {
        LocalDate today = LocalDate.now(clock);
        if (intradayBars == null || today.isBefore(from) || today.isAfter(to)) return List.of();
        LocalTime now = LocalTime.now(clock);
        LocalTime end = now.isBefore(LocalTime.of(9, 0)) ? LocalTime.of(9, 0)
                : now.isAfter(LocalTime.of(15, 30)) ? LocalTime.of(15, 30) : now;
        var source = intradayBars.findBars(code, today, LocalTime.of(9, 0), end,
                BarInterval.ONE_MINUTE);
        Map<LocalTime, List<seokhoon.trade.domain.market.IntradayBar>> groups = new TreeMap<>();
        source.forEach(bar -> groups.computeIfAbsent(bucket(bar.barTime(), minutes), ignored -> new ArrayList<>()).add(bar));
        return groups.entrySet().stream().map(entry -> {
            var bars = entry.getValue(); var first = bars.getFirst(); var last = bars.getLast();
            return new Candle(today, entry.getKey(), first.openPrice(),
                    bars.stream().map(seokhoon.trade.domain.market.IntradayBar::highPrice).max(BigDecimal::compareTo).orElseThrow(),
                    bars.stream().map(seokhoon.trade.domain.market.IntradayBar::lowPrice).min(BigDecimal::compareTo).orElseThrow(),
                    last.closePrice(), bars.stream().mapToLong(seokhoon.trade.domain.market.IntradayBar::volume).sum(),
                    bars.stream().map(seokhoon.trade.domain.market.IntradayBar::tradingValue).reduce(BigDecimal.ZERO, BigDecimal::add));
        }).toList();
    }

    private List<Candle> daily(String code, LocalDate from, LocalDate to, ChartInterval interval) {
        List<DailyPrice> source = dailyPrices.findByStockCodeAndTradeDateBetween(code, from, to)
                .stream().sorted(Comparator.comparing(DailyPrice::tradeDate)).toList();
        if (interval == ChartInterval.DAY) return source.stream().map(Candle::from).toList();
        Map<String, List<DailyPrice>> groups = new LinkedHashMap<>();
        for (DailyPrice price : source) groups.computeIfAbsent(periodKey(price.tradeDate(), interval), ignored -> new ArrayList<>()).add(price);
        return groups.values().stream().map(rows -> {
            DailyPrice first = rows.getFirst(), last = rows.getLast();
            return new Candle(last.tradeDate(), null, first.openPrice(),
                    rows.stream().map(DailyPrice::highPrice).max(BigDecimal::compareTo).orElseThrow(),
                    rows.stream().map(DailyPrice::lowPrice).min(BigDecimal::compareTo).orElseThrow(),
                    last.closePrice(), rows.stream().mapToLong(DailyPrice::volume).sum(),
                    rows.stream().map(DailyPrice::tradingValue).reduce(BigDecimal.ZERO, BigDecimal::add));
        }).toList();
    }

    private List<ChartPoint> indicators(String code, List<Candle> candles) {
        List<DailyPrice> values = candles.stream().map(c -> new DailyPrice(code, c.date(), c.open(), c.high(),
                c.low(), c.close(), c.volume(), c.tradingValue())).toList();
        List<ChartPoint> result = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            List<DailyPrice> history = values.subList(0, i + 1); Candle candle = candles.get(i);
            BigDecimal ma5 = history.size() >= 5 ? calculator.movingAverage(history, 5) : null;
            BigDecimal ma20 = history.size() >= 20 ? calculator.movingAverage(history, 20) : null;
            BigDecimal ma60 = history.size() >= 60 ? calculator.movingAverage(history, 60) : null;
            BigDecimal rsi = history.size() >= 15 ? calculator.rsi(history, 14) : null;
            Macd macd = history.size() >= 35 ? calculator.macd(history) : null;
            BollingerBand band = history.size() >= 20 ? calculator.bollingerBand(history, 20) : null;
            result.add(new ChartPoint(candle.date(), candle.time(), candle.open(), candle.high(), candle.low(), candle.close(),
                    candle.volume(), candle.tradingValue(), ma5, ma20, ma60, rsi,
                    macd == null ? null : macd.macd(), macd == null ? null : macd.signal(),
                    macd == null ? null : macd.histogram(), band == null ? null : band.upper(),
                    band == null ? null : band.middle(), band == null ? null : band.lower()));
        }
        return List.copyOf(result);
    }

    private static LocalTime bucket(LocalTime value, int minutes) {
        int total = value.getHour() * 60 + value.getMinute();
        int marketOpen = 9 * 60;
        int bucket = marketOpen + Math.max(0, total - marketOpen) / minutes * minutes;
        return LocalTime.of(bucket / 60, bucket % 60);
    }

    private static String periodKey(LocalDate date, ChartInterval interval) {
        return switch (interval) {
            case WEEK -> date.getYear() + "-" + date.get(WeekFields.ISO.weekOfWeekBasedYear());
            case MONTH -> date.getYear() + "-" + date.getMonthValue();
            case YEAR -> Integer.toString(date.getYear());
            default -> date.toString();
        };
    }

    private static void validateRange(LocalDate from, LocalDate to) {
        Objects.requireNonNull(from, "from"); Objects.requireNonNull(to, "to");
        if (from.isAfter(to)) throw new IllegalArgumentException("from must not be after to");
        if (from.plusDays(MAX_CALENDAR_DAYS).isBefore(to)) throw new IllegalArgumentException("chart range must be 3650 days or less");
    }

    private static String requireStockCode(String value) {
        if (value == null || !value.matches("[0-9A-Za-z]{1,12}")) throw new IllegalArgumentException("stockCode must be 1-12 letters or digits");
        return value;
    }

    private record Candle(LocalDate date, LocalTime time, BigDecimal open, BigDecimal high,
            BigDecimal low, BigDecimal close, long volume, BigDecimal tradingValue) {
        static Candle from(DailyPrice p) { return new Candle(p.tradeDate(), null, p.openPrice(), p.highPrice(), p.lowPrice(), p.closePrice(), p.volume(), p.tradingValue()); }
    }
}
