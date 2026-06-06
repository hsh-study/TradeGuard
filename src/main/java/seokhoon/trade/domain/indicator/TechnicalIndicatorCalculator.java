package seokhoon.trade.domain.indicator;

import seokhoon.trade.domain.market.DailyPrice;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TechnicalIndicatorCalculator {
    private static final MathContext MC = new MathContext(12, RoundingMode.HALF_UP);
    private static final int SCALE = 4;

    public BigDecimal movingAverage(List<DailyPrice> prices, int period) {
        requirePeriod(prices, period);
        return prices.stream()
                .sorted(Comparator.comparing(DailyPrice::tradeDate).reversed())
                .limit(period)
                .map(DailyPrice::closePrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(period), SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal rsi(List<DailyPrice> prices, int period) {
        List<DailyPrice> sorted = sorted(prices);
        if (sorted.size() < period + 1) {
            throw new IllegalArgumentException("RSI requires period + 1 prices");
        }
        List<DailyPrice> recent = sorted.subList(sorted.size() - period - 1, sorted.size());
        BigDecimal gains = BigDecimal.ZERO;
        BigDecimal losses = BigDecimal.ZERO;
        for (int i = 1; i < recent.size(); i++) {
            BigDecimal diff = recent.get(i).closePrice().subtract(recent.get(i - 1).closePrice());
            if (diff.signum() >= 0) {
                gains = gains.add(diff);
            } else {
                losses = losses.add(diff.abs());
            }
        }
        if (losses.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(100).setScale(SCALE, RoundingMode.HALF_UP);
        }
        BigDecimal avgGain = gains.divide(BigDecimal.valueOf(period), SCALE, RoundingMode.HALF_UP);
        BigDecimal avgLoss = losses.divide(BigDecimal.valueOf(period), SCALE, RoundingMode.HALF_UP);
        BigDecimal rs = avgGain.divide(avgLoss, SCALE, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(100).subtract(BigDecimal.valueOf(100).divide(BigDecimal.ONE.add(rs), SCALE, RoundingMode.HALF_UP))
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    public Macd macd(List<DailyPrice> prices) {
        List<DailyPrice> sorted = sorted(prices);
        if (sorted.size() < 35) {
            throw new IllegalArgumentException("MACD requires at least 35 prices");
        }
        List<BigDecimal> closes = sorted.stream().map(DailyPrice::closePrice).toList();
        List<BigDecimal> ema12 = emaSeries(closes, 12);
        List<BigDecimal> ema26 = emaSeries(closes, 26);
        List<BigDecimal> macdLine = new ArrayList<>();
        for (int i = 0; i < closes.size(); i++) {
            if (ema12.get(i) != null && ema26.get(i) != null) {
                macdLine.add(ema12.get(i).subtract(ema26.get(i), MC));
            }
        }
        List<BigDecimal> signalSeries = emaSeries(macdLine, 9);
        BigDecimal macd = macdLine.get(macdLine.size() - 1).setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal signal = signalSeries.get(signalSeries.size() - 1).setScale(SCALE, RoundingMode.HALF_UP);
        return new Macd(macd, signal, macd.subtract(signal).setScale(SCALE, RoundingMode.HALF_UP));
    }

    public BollingerBand bollingerBand(List<DailyPrice> prices, int period) {
        requirePeriod(prices, period);
        List<BigDecimal> closes = prices.stream()
                .sorted(Comparator.comparing(DailyPrice::tradeDate).reversed())
                .limit(period)
                .map(DailyPrice::closePrice)
                .toList();
        BigDecimal middle = closes.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(period), SCALE, RoundingMode.HALF_UP);
        BigDecimal variance = closes.stream()
                .map(close -> close.subtract(middle, MC).pow(2, MC))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(period), SCALE, RoundingMode.HALF_UP);
        BigDecimal stdDev = BigDecimal.valueOf(Math.sqrt(variance.doubleValue())).setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal width = stdDev.multiply(BigDecimal.valueOf(2), MC).setScale(SCALE, RoundingMode.HALF_UP);
        return new BollingerBand(middle.add(width).setScale(SCALE, RoundingMode.HALF_UP), middle, middle.subtract(width).setScale(SCALE, RoundingMode.HALF_UP));
    }

    public IndicatorSnapshot snapshot(String stockCode, List<DailyPrice> prices) {
        List<DailyPrice> sorted = sorted(prices);
        DailyPrice latest = sorted.get(sorted.size() - 1);
        Macd macd = macd(sorted);
        BollingerBand band = bollingerBand(sorted, 20);
        return new IndicatorSnapshot(stockCode, latest.tradeDate(), movingAverage(sorted, 5), movingAverage(sorted, 20),
                movingAverage(sorted, 60), rsi(sorted, 14), macd.macd(), macd.signal(), macd.histogram(),
                band.upper(), band.middle(), band.lower());
    }

    private static List<BigDecimal> emaSeries(List<BigDecimal> values, int period) {
        if (values.size() < period) {
            throw new IllegalArgumentException("EMA requires enough values");
        }
        BigDecimal multiplier = BigDecimal.valueOf(2).divide(BigDecimal.valueOf(period + 1L), 12, RoundingMode.HALF_UP);
        List<BigDecimal> result = new ArrayList<>();
        BigDecimal ema = null;
        for (int i = 0; i < values.size(); i++) {
            BigDecimal value = values.get(i);
            if (i < period - 1) {
                result.add(null);
            } else if (i == period - 1) {
                ema = values.subList(0, period).stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(period), 12, RoundingMode.HALF_UP);
                result.add(ema);
            } else {
                ema = value.subtract(ema, MC).multiply(multiplier, MC).add(ema, MC);
                result.add(ema);
            }
        }
        return result;
    }

    private static void requirePeriod(List<DailyPrice> prices, int period) {
        if (prices == null || prices.size() < period) {
            throw new IllegalArgumentException("At least " + period + " prices are required");
        }
    }

    private static List<DailyPrice> sorted(List<DailyPrice> prices) {
        if (prices == null || prices.isEmpty()) {
            throw new IllegalArgumentException("prices must not be empty");
        }
        return prices.stream().sorted(Comparator.comparing(DailyPrice::tradeDate)).toList();
    }
}
