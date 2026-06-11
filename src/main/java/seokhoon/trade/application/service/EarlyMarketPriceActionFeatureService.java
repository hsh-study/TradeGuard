package seokhoon.trade.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import seokhoon.trade.application.port.in.LoadEarlyMarketPriceActionFeaturesUseCase;
import seokhoon.trade.application.port.out.DailyPricePort;
import seokhoon.trade.application.port.out.IntradayBarPort;
import seokhoon.trade.application.port.out.MarketCalendarPort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.domain.market.BarInterval;
import seokhoon.trade.domain.market.DailyPrice;
import seokhoon.trade.domain.market.EarlyMarketPriceActionFeatures;
import seokhoon.trade.domain.market.IntradayBar;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class EarlyMarketPriceActionFeatureService
        implements LoadEarlyMarketPriceActionFeaturesUseCase {
    private static final Logger log =
            LoggerFactory.getLogger(EarlyMarketPriceActionFeatureService.class);
    private static final LocalTime MARKET_OPEN = LocalTime.of(9, 0);

    private final DailyPricePort dailyPricePort;
    private final IntradayBarPort intradayBarPort;
    private final MarketCalendarPort marketCalendarPort;
    private final OperationalMetricsPort metricsPort;

    public EarlyMarketPriceActionFeatureService(
            DailyPricePort dailyPricePort,
            IntradayBarPort intradayBarPort,
            MarketCalendarPort marketCalendarPort,
            OperationalMetricsPort metricsPort
    ) {
        this.dailyPricePort = dailyPricePort;
        this.intradayBarPort = intradayBarPort;
        this.marketCalendarPort = marketCalendarPort;
        this.metricsPort = metricsPort;
    }

    @Override
    public EarlyMarketPriceActionFeatures load(
            String stockCode,
            LocalDate tradeDate,
            LocalTime to
    ) {
        if (stockCode == null || stockCode.isBlank()) {
            throw new IllegalArgumentException("stockCode must not be blank");
        }
        Objects.requireNonNull(tradeDate, "tradeDate");
        Objects.requireNonNull(to, "to");
        if (to.isBefore(MARKET_OPEN)) {
            throw new IllegalArgumentException("to must not be before market open");
        }

        LocalDate previousTradingDay = marketCalendarPort.previousTradingDay(tradeDate);
        Optional<DailyPrice> previousPrice =
                loadPreviousPrice(stockCode, previousTradingDay);
        List<IntradayBar> bars = loadBars(stockCode, tradeDate, to);
        List<String> reasons = new ArrayList<>();

        if (previousPrice.isEmpty()) {
            reasons.add("PREVIOUS_HIGH_UNAVAILABLE");
        }
        if (bars.isEmpty()) {
            reasons.add("INTRADAY_BARS_UNAVAILABLE");
        }
        if (previousPrice.isEmpty() || bars.isEmpty()) {
            metricsPort.recordEarlyMarketPriceAction("insufficient");
            EarlyMarketPriceActionFeatures features = new EarlyMarketPriceActionFeatures(
                    stockCode,
                    tradeDate,
                    previousTradingDay,
                    previousPrice.map(DailyPrice::highPrice).orElse(null),
                    bars.isEmpty() ? null : bars.getFirst().openPrice(),
                    bars.isEmpty() ? null : bars.getLast().closePrice(),
                    null,
                    null,
                    null,
                    false,
                    List.copyOf(reasons)
            );
            logFeatures(features, "insufficient");
            return features;
        }

        BigDecimal previousHigh = previousPrice.orElseThrow().highPrice();
        BigDecimal openingPrice = bars.getFirst().openPrice();
        BigDecimal lastPrice = bars.getLast().closePrice();
        boolean brokePreviousHigh = bars.stream()
                .map(IntradayBar::highPrice)
                .anyMatch(high -> high.compareTo(previousHigh) >= 0);
        boolean heldOpeningPrice = lastPrice.compareTo(openingPrice) >= 0;
        boolean tradedBelowOpening = bars.stream()
                .map(IntradayBar::lowPrice)
                .anyMatch(low -> low.compareTo(openingPrice) < 0);
        boolean pullbackRecovered = tradedBelowOpening && heldOpeningPrice;

        reasons.add(brokePreviousHigh
                ? "PREVIOUS_HIGH_BROKEN"
                : "PREVIOUS_HIGH_NOT_BROKEN");
        reasons.add(heldOpeningPrice
                ? "OPENING_PRICE_HELD"
                : "OPENING_PRICE_LOST");
        if (pullbackRecovered) {
            reasons.add("PULLBACK_RECOVERED");
        }
        metricsPort.recordEarlyMarketPriceAction("sufficient");
        EarlyMarketPriceActionFeatures features = new EarlyMarketPriceActionFeatures(
                stockCode,
                tradeDate,
                previousTradingDay,
                previousHigh,
                openingPrice,
                lastPrice,
                brokePreviousHigh,
                heldOpeningPrice,
                pullbackRecovered,
                true,
                List.copyOf(reasons)
        );
        logFeatures(features, "sufficient");
        return features;
    }

    private Optional<DailyPrice> loadPreviousPrice(
            String stockCode,
            LocalDate previousTradingDay
    ) {
        try {
            return dailyPricePort.findByStockCodeAndTradeDateBetween(
                            stockCode,
                            previousTradingDay,
                            previousTradingDay
                    )
                    .stream()
                    .filter(price -> price.tradeDate().equals(previousTradingDay))
                    .findFirst();
        } catch (RuntimeException exception) {
            log.atWarn()
                    .addKeyValue("stockCode", stockCode)
                    .addKeyValue("tradeDate", previousTradingDay)
                    .addKeyValue("result", "failure")
                    .addKeyValue("errorType", exception.getClass().getSimpleName())
                    .log("Previous daily price lookup failed");
            return Optional.empty();
        }
    }

    private List<IntradayBar> loadBars(
            String stockCode,
            LocalDate tradeDate,
            LocalTime to
    ) {
        try {
            List<IntradayBar> bars = intradayBarPort.findBars(
                    stockCode,
                    tradeDate,
                    MARKET_OPEN,
                    to,
                    BarInterval.ONE_MINUTE
            ).stream()
                    .sorted(Comparator.comparing(IntradayBar::barTime))
                    .toList();
            metricsPort.recordIntradayBarLookup(
                    bars.isEmpty() ? "not_found" : "found"
            );
            return bars;
        } catch (RuntimeException exception) {
            metricsPort.recordIntradayBarLookup("failure");
            log.atWarn()
                    .addKeyValue("stockCode", stockCode)
                    .addKeyValue("tradeDate", tradeDate)
                    .addKeyValue("result", "failure")
                    .addKeyValue("errorType", exception.getClass().getSimpleName())
                    .log("Early market price action bar lookup failed");
            return List.of();
        }
    }

    private static void logFeatures(
            EarlyMarketPriceActionFeatures features,
            String result
    ) {
        log.atInfo()
                .addKeyValue("stockCode", features.stockCode())
                .addKeyValue("tradeDate", features.tradeDate())
                .addKeyValue("previousTradingDay", features.previousTradingDay())
                .addKeyValue("brokePreviousHigh", features.brokePreviousHigh())
                .addKeyValue("heldOpeningPrice", features.heldOpeningPrice())
                .addKeyValue("pullbackRecovered", features.pullbackRecovered())
                .addKeyValue("result", result)
                .log("Early market price action features calculated");
    }
}
