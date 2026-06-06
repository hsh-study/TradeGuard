package seokhoon.trade.domain.strategy;

import seokhoon.trade.domain.indicator.IndicatorSnapshot;
import seokhoon.trade.domain.market.DailyPrice;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ClosingBetStrategy {
    public static final String STRATEGY_NAME = "CLOSING_BET";
    private static final BigDecimal FIFTY_BILLION = BigDecimal.valueOf(50_000_000_000L);

    public TradingSignal evaluate(List<DailyPrice> prices, IndicatorSnapshot indicator) {
        List<DailyPrice> sorted = prices.stream().sorted(Comparator.comparing(DailyPrice::tradeDate)).toList();
        if (sorted.size() < 20) {
            throw new IllegalArgumentException("ClosingBetStrategy requires at least 20 prices");
        }
        DailyPrice today = sorted.getLast();
        DailyPrice yesterday = sorted.get(sorted.size() - 2);
        BigDecimal avgVolume20 = sorted.subList(sorted.size() - 20, sorted.size()).stream()
                .map(price -> BigDecimal.valueOf(price.volume()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(20), 4, RoundingMode.HALF_UP);

        int score = 0;
        List<String> reasons = new ArrayList<>();
        if (indicator.ma5().compareTo(indicator.ma20()) > 0) {
            score += 15;
            reasons.add("MA5_ABOVE_MA20");
        }
        if (today.closePrice().compareTo(indicator.ma20()) > 0) {
            score += 10;
            reasons.add("CLOSE_ABOVE_MA20");
        }
        if (BigDecimal.valueOf(today.volume()).compareTo(avgVolume20.multiply(BigDecimal.valueOf(2))) >= 0) {
            score += 20;
            reasons.add("VOLUME_SPIKE_20D_200PCT");
        }
        if (closeLocation(today).compareTo(BigDecimal.valueOf(0.8)) >= 0) {
            score += 15;
            reasons.add("CLOSE_NEAR_HIGH");
        }
        if (upperTailRate(today).compareTo(BigDecimal.valueOf(0.05)) >= 0) {
            score -= 15;
            reasons.add("LONG_UPPER_TAIL");
        }
        if (dailyChangeRate(yesterday, today).compareTo(BigDecimal.valueOf(0.15)) >= 0) {
            score -= 10;
            reasons.add("SHARP_RISE_FROM_PREVIOUS_CLOSE");
        }
        if (today.tradingValue().compareTo(FIFTY_BILLION) >= 0) {
            score += 15;
            reasons.add("TRADING_VALUE_OVER_50B_KRW");
        }
        return new TradingSignal(STRATEGY_NAME, today.stockCode(), today.tradeDate(), SignalType.BUY_CANDIDATE, score, reasons);
    }

    private static BigDecimal closeLocation(DailyPrice price) {
        BigDecimal range = price.highPrice().subtract(price.lowPrice());
        if (range.signum() == 0) {
            return BigDecimal.ONE;
        }
        return price.closePrice().subtract(price.lowPrice()).divide(range, 4, RoundingMode.HALF_UP);
    }

    private static BigDecimal upperTailRate(DailyPrice price) {
        if (price.closePrice().signum() == 0) {
            return BigDecimal.ZERO;
        }
        return price.highPrice().subtract(price.closePrice()).divide(price.closePrice(), 4, RoundingMode.HALF_UP);
    }

    private static BigDecimal dailyChangeRate(DailyPrice yesterday, DailyPrice today) {
        if (yesterday.closePrice().signum() == 0) {
            return BigDecimal.ZERO;
        }
        return today.closePrice().subtract(yesterday.closePrice()).divide(yesterday.closePrice(), 4, RoundingMode.HALF_UP);
    }
}
