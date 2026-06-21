package seokhoon.trade.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import seokhoon.trade.application.port.in.GetWatchlistPortfolioUseCase;
import seokhoon.trade.application.port.out.DailyPricePort;
import seokhoon.trade.application.port.out.LivePositionPort;
import seokhoon.trade.application.port.out.KisAccountBalancePort;
import seokhoon.trade.application.port.out.StockPort;
import seokhoon.trade.application.port.out.TradingSignalQueryPort;
import seokhoon.trade.application.port.out.TradingSignalRecord;
import seokhoon.trade.application.port.out.ValuationSnapshotPort;
import seokhoon.trade.application.port.in.TradingSignalSearchCriteria;
import seokhoon.trade.domain.kis.KisEnvironment;
import seokhoon.trade.domain.market.DailyPrice;
import seokhoon.trade.domain.position.LivePosition;
import seokhoon.trade.domain.stock.Stock;
import seokhoon.trade.domain.strategy.SignalType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WatchlistPortfolioService implements GetWatchlistPortfolioUseCase {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final String EARLY_MARKET_TAG = "장초반";
    private static final String CLOSING_BET_TAG = "종베";
    private static final String USER_TAG = "사용자";
    private final StockPort stocks;
    private final DailyPricePort prices;
    private final LivePositionPort positions;
    private final TradingSignalQueryPort signals;
    private final KisAccountBalancePort accountBalances;
    private final ValuationSnapshotPort valuations;
    private final Clock clock;

    @Autowired
    public WatchlistPortfolioService(StockPort stocks, DailyPricePort prices,
            LivePositionPort positions, TradingSignalQueryPort signals,
            KisAccountBalancePort accountBalances, ValuationSnapshotPort valuations) {
        this(stocks, prices, positions, signals, accountBalances, valuations, Clock.system(SEOUL));
    }

    WatchlistPortfolioService(StockPort stocks, DailyPricePort prices,
            LivePositionPort positions, TradingSignalQueryPort signals) {
        this(stocks, prices, positions, signals, ignored -> List.of(), null, Clock.system(SEOUL));
    }

    WatchlistPortfolioService(StockPort stocks, DailyPricePort prices,
            LivePositionPort positions, TradingSignalQueryPort signals, Clock clock) {
        this(stocks, prices, positions, signals, ignored -> List.of(), null, clock);
    }

    WatchlistPortfolioService(StockPort stocks, DailyPricePort prices,
            LivePositionPort positions, TradingSignalQueryPort signals,
            KisAccountBalancePort accountBalances, Clock clock) {
        this(stocks, prices, positions, signals, accountBalances, null, clock);
    }

    WatchlistPortfolioService(StockPort stocks, DailyPricePort prices,
            LivePositionPort positions, TradingSignalQueryPort signals,
            KisAccountBalancePort accountBalances, ValuationSnapshotPort valuations, Clock clock) {
        this.stocks = stocks;
        this.prices = prices;
        this.positions = positions;
        this.signals = signals;
        this.accountBalances = accountBalances;
        this.valuations = valuations;
        this.clock = clock;
    }

    @Override
    public java.util.List<WatchlistItem> watchlist() {
        List<Stock> stockList = stocks.findAll().stream().filter(Stock::active).toList();
        Map<String, DailyPrice> latestPrices = latestPrices(stockList);
        List<LivePosition> openPositions = positions.findOpenPositions();
        Map<String, List<String>> tags = tags(latestPrices, openPositions);
        return stockList.stream().map(stock -> {
            DailyPrice latest = latestPrices.get(stock.stockCode());
            var valuation = valuation(stock.stockCode());
            return new WatchlistItem(stock.stockCode(), stock.stockName(), stock.market(),
                    stock.active(), latest == null ? null : latest.tradeDate(),
                    latest == null ? null : latest.closePrice(),
                    latest == null ? null : latest.volume(),
                    valuation.per(), valuation.pbr(),
                    tags.getOrDefault(stock.stockCode(), List.of()));
        }).toList();
    }

    @Override
    public java.util.List<HoldingItem> holdings() {
        List<Stock> stockList = stocks.findAll();
        Map<String, Stock> stockMap = stockList.stream()
                .collect(Collectors.toMap(Stock::stockCode, Function.identity(), (a, b) -> a));
        Map<String, DailyPrice> latestPrices = latestPrices(stockList);
        List<LivePosition> openPositions = positions.findOpenPositions();
        Map<String, List<String>> tags = tags(latestPrices, openPositions);
        return openPositions.stream().map(position -> {
            DailyPrice latest = latestPrices.computeIfAbsent(position.stockCode(), code ->
                    prices.findLatestByStockCode(code).orElse(null));
            BigDecimal close = latest == null ? null : latest.closePrice();
            BigDecimal marketValue = close == null ? null
                    : close.multiply(BigDecimal.valueOf(position.quantity()));
            BigDecimal profitLoss = marketValue == null ? null
                    : marketValue.subtract(position.buyAmount());
            BigDecimal returnRate = profitLoss == null || position.buyAmount().signum() == 0
                    ? null : profitLoss.multiply(BigDecimal.valueOf(100))
                    .divide(position.buyAmount(), 4, RoundingMode.HALF_UP);
            Stock stock = stockMap.get(position.stockCode());
            var valuation = valuation(position.stockCode());
            return new HoldingItem(position.id(), position.environment(),
                    label(position.environment()), position.stockCode(),
                    stock == null ? position.stockCode() : stock.stockName(),
                    position.quantity(), position.averageBuyPrice(), position.buyAmount(),
                    latest == null ? null : latest.tradeDate(), close, marketValue,
                    profitLoss, returnRate, valuation.per(), valuation.pbr(), position.openedAt(),
                    tags.getOrDefault(position.stockCode(), List.of(USER_TAG)), "LOCAL_POSITION");
        }).toList();
    }

    @Override
    public List<HoldingItem> holdings(long accountId) {
        var rows = accountBalances.holdings(accountId);
        Map<String, LivePosition> local = positions.findOpenPositions().stream()
                .filter(position -> position.environment() != null)
                .collect(Collectors.toMap(position -> position.environment() + ":" + position.stockCode(),
                        Function.identity(), (a, b) -> a));
        return rows.stream().map(row -> { var valuation = valuation(row.stockCode()); return new HoldingItem(
                java.util.Optional.ofNullable(local.get(row.environment() + ":" + row.stockCode()))
                        .map(LivePosition::id).orElse(null), row.environment(),
                label(row.environment()),
                row.stockCode(), row.stockName(), row.quantity(),
                row.averageBuyPrice(), row.buyAmount(), null, row.currentPrice(),
                row.marketValue(), row.unrealizedProfitLoss(), row.unrealizedReturnRate(),
                valuation.per(), valuation.pbr(), null, List.of(USER_TAG), "KIS_ACCOUNT"); }).toList();
    }

    private Valuation valuation(String stockCode) {
        if (valuations == null) return new Valuation(null, null);
        return valuations.findLatestByStockCode(stockCode, LocalDate.now(clock))
                .map(value -> new Valuation(value.per(), value.pbr()))
                .orElseGet(() -> new Valuation(null, null));
    }

    private record Valuation(BigDecimal per, BigDecimal pbr) {}

    private Map<String, DailyPrice> latestPrices(List<Stock> stockList) {
        Map<String, DailyPrice> result = new HashMap<>();
        stockList.forEach(stock -> prices.findLatestByStockCode(stock.stockCode())
                .ifPresent(price -> result.put(stock.stockCode(), price)));
        return result;
    }

    private Map<String, List<String>> tags(Map<String, DailyPrice> latestPrices,
            List<LivePosition> openPositions) {
        Map<String, Set<String>> values = new HashMap<>();
        openPositions.forEach(position -> values
                .computeIfAbsent(position.stockCode(), ignored -> new HashSet<>())
                .add(USER_TAG));

        Set<LocalDate> relevantDates = new HashSet<>();
        relevantDates.add(LocalDate.now(clock));
        latestPrices.values().stream().map(DailyPrice::tradeDate)
                .max(LocalDate::compareTo).ifPresent(relevantDates::add);
        relevantDates.forEach(date -> {
            addRecommendationTags(values, date, SignalType.EARLY_MARKET_ENTRY_CANDIDATE,
                    "EARLY_MARKET_BREAKOUT", EARLY_MARKET_TAG);
            addRecommendationTags(values, date, SignalType.BUY_CANDIDATE,
                    "CLOSING_BET", CLOSING_BET_TAG);
        });

        Map<String, List<String>> result = new HashMap<>();
        values.forEach((stockCode, stockTags) -> {
            List<String> ordered = new ArrayList<>();
            List.of(EARLY_MARKET_TAG, CLOSING_BET_TAG, USER_TAG).stream()
                    .filter(stockTags::contains).forEach(ordered::add);
            result.put(stockCode, List.copyOf(ordered));
        });
        return result;
    }

    private void addRecommendationTags(Map<String, Set<String>> values, LocalDate date,
            SignalType signalType, String strategyName, String tag) {
        signals.find(new TradingSignalSearchCriteria(null, date, strategyName,
                        signalType, null, null)).stream()
                .map(TradingSignalRecord::stockCode)
                .forEach(stockCode -> values
                        .computeIfAbsent(stockCode, ignored -> new HashSet<>()).add(tag));
    }

    private static String label(KisEnvironment environment) {
        if (environment == null) return "미분류";
        return environment == KisEnvironment.DEMO ? "모의투자" : "실전투자";
    }
}
