package seokhoon.trade.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import seokhoon.trade.application.port.in.CaptureEarlyMarketFollowUpDataUseCase;
import seokhoon.trade.application.port.in.CaptureEarlyMarketOpeningDataUseCase;
import seokhoon.trade.application.port.in.CaptureEarlyMarketPerformanceDataUseCase;
import seokhoon.trade.application.port.in.CaptureEarlyMarketPreOpenDataUseCase;
import seokhoon.trade.application.port.in.EarlyMarketDataCaptureResult;
import seokhoon.trade.application.port.in.LoadEarlyMarketDataArchiveUseCase;
import seokhoon.trade.application.port.in.TradingSignalSearchCriteria;
import seokhoon.trade.application.port.out.AfterHoursMarketDataPort;
import seokhoon.trade.application.port.out.EarlyMarketAfterHoursSnapshotPort;
import seokhoon.trade.application.port.out.EarlyMarketDataCapturePort;
import seokhoon.trade.application.port.out.EarlyMarketIntradayBarSnapshotPort;
import seokhoon.trade.application.port.out.EarlyMarketMarketSnapshotArchivePort;
import seokhoon.trade.application.port.out.EarlyMarketRankingSnapshotPort;
import seokhoon.trade.application.port.out.IntradayBarPort;
import seokhoon.trade.application.port.out.IntradayMarketSnapshot;
import seokhoon.trade.application.port.out.MarketCalendarPort;
import seokhoon.trade.application.port.out.MarketRankingPort;
import seokhoon.trade.application.port.out.MarketRankingStock;
import seokhoon.trade.application.port.out.MarketSnapshotPort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.application.port.out.TradingSignalQueryPort;
import seokhoon.trade.application.port.out.TradingSignalRecord;
import seokhoon.trade.domain.market.AfterHoursQuote;
import seokhoon.trade.domain.market.BarInterval;
import seokhoon.trade.domain.market.EarlyMarketAfterHoursSnapshot;
import seokhoon.trade.domain.market.EarlyMarketCaptureStatus;
import seokhoon.trade.domain.market.EarlyMarketCaptureType;
import seokhoon.trade.domain.market.EarlyMarketDataCapture;
import seokhoon.trade.domain.market.EarlyMarketIntradayBarSnapshot;
import seokhoon.trade.domain.market.EarlyMarketMarketSnapshot;
import seokhoon.trade.domain.market.EarlyMarketRankingSnapshot;
import seokhoon.trade.domain.market.EarlyMarketSnapshotType;
import seokhoon.trade.domain.market.IntradayBar;
import seokhoon.trade.domain.stock.Market;
import seokhoon.trade.domain.strategy.SignalType;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class EarlyMarketDataArchiveService
        implements CaptureEarlyMarketPreOpenDataUseCase,
        CaptureEarlyMarketOpeningDataUseCase,
        CaptureEarlyMarketFollowUpDataUseCase,
        CaptureEarlyMarketPerformanceDataUseCase,
        LoadEarlyMarketDataArchiveUseCase {
    private static final Logger log =
            LoggerFactory.getLogger(EarlyMarketDataArchiveService.class);
    private static final int RANKING_LIMIT = 10;
    private static final List<Market> MARKETS =
            List.of(Market.KOSPI, Market.KOSDAQ);
    private static final String RANKING_SOURCE = "MARKET_RANKING_PORT";
    private static final String AFTER_HOURS_SOURCE = "AFTER_HOURS_MARKET_DATA_PORT";
    private static final String BAR_SOURCE = "INTRADAY_BAR_PORT";
    private static final String SNAPSHOT_SOURCE = "MARKET_SNAPSHOT_PORT";

    private final MarketRankingPort marketRankingPort;
    private final AfterHoursMarketDataPort afterHoursMarketDataPort;
    private final IntradayBarPort intradayBarPort;
    private final MarketSnapshotPort marketSnapshotPort;
    private final MarketCalendarPort marketCalendarPort;
    private final TradingSignalQueryPort tradingSignalQueryPort;
    private final EarlyMarketDataCapturePort capturePort;
    private final EarlyMarketRankingSnapshotPort rankingSnapshotPort;
    private final EarlyMarketAfterHoursSnapshotPort afterHoursSnapshotPort;
    private final EarlyMarketIntradayBarSnapshotPort barSnapshotPort;
    private final EarlyMarketMarketSnapshotArchivePort marketSnapshotArchivePort;
    private final OperationalMetricsPort metricsPort;
    private final Clock clock;

    @Autowired
    public EarlyMarketDataArchiveService(
            MarketRankingPort marketRankingPort,
            AfterHoursMarketDataPort afterHoursMarketDataPort,
            IntradayBarPort intradayBarPort,
            MarketSnapshotPort marketSnapshotPort,
            MarketCalendarPort marketCalendarPort,
            TradingSignalQueryPort tradingSignalQueryPort,
            EarlyMarketDataCapturePort capturePort,
            EarlyMarketRankingSnapshotPort rankingSnapshotPort,
            EarlyMarketAfterHoursSnapshotPort afterHoursSnapshotPort,
            EarlyMarketIntradayBarSnapshotPort barSnapshotPort,
            EarlyMarketMarketSnapshotArchivePort marketSnapshotArchivePort,
            OperationalMetricsPort metricsPort
    ) {
        this(
                marketRankingPort,
                afterHoursMarketDataPort,
                intradayBarPort,
                marketSnapshotPort,
                marketCalendarPort,
                tradingSignalQueryPort,
                capturePort,
                rankingSnapshotPort,
                afterHoursSnapshotPort,
                barSnapshotPort,
                marketSnapshotArchivePort,
                metricsPort,
                Clock.systemUTC()
        );
    }

    EarlyMarketDataArchiveService(
            MarketRankingPort marketRankingPort,
            AfterHoursMarketDataPort afterHoursMarketDataPort,
            IntradayBarPort intradayBarPort,
            MarketSnapshotPort marketSnapshotPort,
            MarketCalendarPort marketCalendarPort,
            TradingSignalQueryPort tradingSignalQueryPort,
            EarlyMarketDataCapturePort capturePort,
            EarlyMarketRankingSnapshotPort rankingSnapshotPort,
            EarlyMarketAfterHoursSnapshotPort afterHoursSnapshotPort,
            EarlyMarketIntradayBarSnapshotPort barSnapshotPort,
            EarlyMarketMarketSnapshotArchivePort marketSnapshotArchivePort,
            OperationalMetricsPort metricsPort,
            Clock clock
    ) {
        this.marketRankingPort = marketRankingPort;
        this.afterHoursMarketDataPort = afterHoursMarketDataPort;
        this.intradayBarPort = intradayBarPort;
        this.marketSnapshotPort = marketSnapshotPort;
        this.marketCalendarPort = marketCalendarPort;
        this.tradingSignalQueryPort = tradingSignalQueryPort;
        this.capturePort = capturePort;
        this.rankingSnapshotPort = rankingSnapshotPort;
        this.afterHoursSnapshotPort = afterHoursSnapshotPort;
        this.barSnapshotPort = barSnapshotPort;
        this.marketSnapshotArchivePort = marketSnapshotArchivePort;
        this.metricsPort = metricsPort;
        this.clock = clock;
    }

    @Override
    public EarlyMarketDataCaptureResult capturePreOpen(LocalDate tradeDate) {
        Objects.requireNonNull(tradeDate, "tradeDate");
        Instant capturedAt = clock.instant();
        RankingLoad rankingLoad = loadRankings(tradeDate, capturedAt);
        EarlyMarketDataCapture rankingCapture = persistRankings(
                tradeDate,
                capturedAt,
                rankingLoad
        );
        EarlyMarketDataCapture afterHoursCapture = captureAfterHours(
                tradeDate,
                capturedAt,
                rankingLoad.uniqueStockCodes()
        );
        return new EarlyMarketDataCaptureResult(
                tradeDate,
                List.of(rankingCapture, afterHoursCapture)
        );
    }

    @Override
    public EarlyMarketDataCaptureResult captureOpening(LocalDate tradeDate) {
        List<String> stockCodes = signalStockCodes(
                tradeDate,
                SignalType.EARLY_MARKET_PRE_SCAN
        );
        Instant capturedAt = clock.instant();
        EarlyMarketDataCapture bars = captureBars(
                tradeDate,
                stockCodes,
                LocalTime.of(9, 0),
                LocalTime.of(9, 5),
                EarlyMarketCaptureType.OPENING_BARS_0900_0930,
                capturedAt
        );
        EarlyMarketDataCapture snapshots = captureMarketSnapshots(
                tradeDate,
                stockCodes,
                EarlyMarketSnapshotType.OPENING_0905,
                EarlyMarketCaptureType.OPENING_SNAPSHOT_0905,
                capturedAt,
                false
        );
        return new EarlyMarketDataCaptureResult(
                tradeDate,
                List.of(bars, snapshots)
        );
    }

    @Override
    public EarlyMarketDataCaptureResult captureFollowUp(LocalDate tradeDate) {
        List<String> stockCodes = signalStockCodes(
                tradeDate,
                SignalType.EARLY_MARKET_ENTRY_CANDIDATE
        );
        Instant capturedAt = clock.instant();
        StageArchive archive = loadStageArchive(
                tradeDate,
                stockCodes,
                LocalTime.of(9, 5),
                LocalTime.of(9, 20),
                EarlyMarketSnapshotType.FOLLOW_UP_0920,
                capturedAt,
                true
        );
        return new EarlyMarketDataCaptureResult(
                tradeDate,
                List.of(saveCapture(
                        tradeDate,
                        EarlyMarketCaptureType.FOLLOW_UP_BARS_0905_0920,
                        capturedAt,
                        "INTRADAY_BAR_PORT+MARKET_SNAPSHOT_PORT",
                        archive.status(),
                        archive.itemCount(),
                        archive.failureReason()
                ))
        );
    }

    @Override
    public EarlyMarketDataCaptureResult capturePerformance(LocalDate tradeDate) {
        List<String> stockCodes = new ArrayList<>(signalStockCodes(
                tradeDate,
                SignalType.EARLY_MARKET_PRE_SCAN
        ));
        signalStockCodes(tradeDate, SignalType.EARLY_MARKET_ENTRY_CANDIDATE)
                .forEach(stockCode -> {
                    if (!stockCodes.contains(stockCode)) {
                        stockCodes.add(stockCode);
                    }
                });
        Instant capturedAt = clock.instant();
        StageArchive archive = loadStageArchive(
                tradeDate,
                stockCodes,
                LocalTime.of(9, 0),
                LocalTime.of(9, 30),
                EarlyMarketSnapshotType.PERFORMANCE_0930,
                capturedAt,
                true
        );
        return new EarlyMarketDataCaptureResult(
                tradeDate,
                List.of(saveCapture(
                        tradeDate,
                        EarlyMarketCaptureType.PERFORMANCE_BARS_0900_0930,
                        capturedAt,
                        "INTRADAY_BAR_PORT+MARKET_SNAPSHOT_PORT",
                        archive.status(),
                        archive.itemCount(),
                        archive.failureReason()
                ))
        );
    }

    @Override
    public List<EarlyMarketDataCapture> loadCaptures(LocalDate tradeDate) {
        return capturePort.findCaptures(requireDate(tradeDate));
    }

    @Override
    public List<EarlyMarketRankingSnapshot> loadRankings(LocalDate tradeDate) {
        return rankingSnapshotPort.findRankings(requireDate(tradeDate));
    }

    @Override
    public List<EarlyMarketAfterHoursSnapshot> loadAfterHours(
            LocalDate tradeDate
    ) {
        return afterHoursSnapshotPort.findAfterHours(requireDate(tradeDate));
    }

    @Override
    public List<EarlyMarketIntradayBarSnapshot> loadBars(
            LocalDate tradeDate,
            String stockCode
    ) {
        return barSnapshotPort.findBars(
                requireDate(tradeDate),
                requireStockCode(stockCode)
        );
    }

    @Override
    public List<EarlyMarketMarketSnapshot> loadMarketSnapshots(
            LocalDate tradeDate,
            String stockCode
    ) {
        return marketSnapshotArchivePort.findMarketSnapshots(
                requireDate(tradeDate),
                requireStockCode(stockCode)
        );
    }

    private RankingLoad loadRankings(LocalDate tradeDate, Instant capturedAt) {
        List<EarlyMarketRankingSnapshot> snapshots = new ArrayList<>();
        int failures = 0;
        for (Market market : MARKETS) {
            failures += loadRanking(
                    tradeDate,
                    capturedAt,
                    market,
                    "TRADING_VALUE",
                    () -> marketRankingPort.findTopTradingValueStocks(
                            market,
                            RANKING_LIMIT
                    ),
                    snapshots
            );
            failures += loadRanking(
                    tradeDate,
                    capturedAt,
                    market,
                    "RISING",
                    () -> marketRankingPort.findTopRisingStocks(
                            market,
                            RANKING_LIMIT
                    ),
                    snapshots
            );
            failures += loadRanking(
                    tradeDate,
                    capturedAt,
                    market,
                    "VOLUME_SURGE",
                    () -> marketRankingPort.findVolumeSurgeStocks(
                            market,
                            RANKING_LIMIT
                    ),
                    snapshots
            );
        }
        Map<String, String> unique = new LinkedHashMap<>();
        snapshots.forEach(snapshot -> unique.putIfAbsent(
                snapshot.stockCode(),
                snapshot.stockCode()
        ));
        return new RankingLoad(
                List.copyOf(snapshots),
                List.copyOf(unique.values()),
                failures
        );
    }

    private int loadRanking(
            LocalDate tradeDate,
            Instant capturedAt,
            Market market,
            String rankingType,
            RankingSupplier supplier,
            List<EarlyMarketRankingSnapshot> target
    ) {
        try {
            List<MarketRankingStock> stocks = supplier.get();
            for (int index = 0; index < stocks.size(); index++) {
                MarketRankingStock stock = stocks.get(index);
                target.add(new EarlyMarketRankingSnapshot(
                        null,
                        tradeDate,
                        capturedAt,
                        index + 1,
                        stock.stockCode(),
                        stock.stockName(),
                        stock.currentPrice(),
                        stock.changeRate(),
                        stock.volume(),
                        stock.tradingValue(),
                        RANKING_SOURCE + ":" + market.name() + ":" + rankingType
                ));
            }
            return 0;
        } catch (RuntimeException exception) {
            return 1;
        }
    }

    private EarlyMarketDataCapture persistRankings(
            LocalDate tradeDate,
            Instant capturedAt,
            RankingLoad load
    ) {
        int itemCount = 0;
        int failures = load.failures();
        try {
            itemCount = rankingSnapshotPort.saveAll(load.snapshots()).size();
        } catch (RuntimeException exception) {
            failures++;
        }
        return saveCapture(
                tradeDate,
                EarlyMarketCaptureType.PRE_OPEN_RANKING_0830,
                capturedAt,
                RANKING_SOURCE,
                rankingStatus(itemCount, load.failures(), failures),
                itemCount,
                failureReason(failures, 0)
        );
    }

    private EarlyMarketDataCapture captureAfterHours(
            LocalDate tradeDate,
            Instant capturedAt,
            List<String> stockCodes
    ) {
        if (stockCodes.isEmpty()) {
            return saveCapture(
                    tradeDate,
                    EarlyMarketCaptureType.AFTER_HOURS_PREVIOUS_DAY,
                    capturedAt,
                    AFTER_HOURS_SOURCE,
                    EarlyMarketCaptureStatus.SKIPPED,
                    0,
                    null
            );
        }
        LocalDate previousTradingDay =
                marketCalendarPort.previousTradingDay(tradeDate);
        List<EarlyMarketAfterHoursSnapshot> snapshots = new ArrayList<>();
        int failures = 0;
        int missing = 0;
        for (String stockCode : stockCodes) {
            try {
                Optional<AfterHoursQuote> quote = afterHoursMarketDataPort
                        .findByStockCode(stockCode, previousTradingDay);
                if (quote.isPresent()) {
                    snapshots.add(toAfterHoursSnapshot(
                            tradeDate,
                            previousTradingDay,
                            quote.orElseThrow(),
                            capturedAt
                    ));
                } else {
                    missing++;
                }
            } catch (RuntimeException exception) {
                failures++;
            }
        }
        int saved = 0;
        try {
            saved = afterHoursSnapshotPort.upsertAfterHours(snapshots).size();
        } catch (RuntimeException exception) {
            failures++;
        }
        return saveCapture(
                tradeDate,
                EarlyMarketCaptureType.AFTER_HOURS_PREVIOUS_DAY,
                capturedAt,
                AFTER_HOURS_SOURCE,
                status(stockCodes.size(), snapshots.size(), failures + missing),
                saved,
                failureReason(failures, missing)
        );
    }

    private EarlyMarketDataCapture captureBars(
            LocalDate tradeDate,
            List<String> stockCodes,
            LocalTime from,
            LocalTime to,
            EarlyMarketCaptureType captureType,
            Instant capturedAt
    ) {
        StageArchive archive = loadStageArchive(
                tradeDate,
                stockCodes,
                from,
                to,
                null,
                capturedAt,
                false
        );
        return saveCapture(
                tradeDate,
                captureType,
                capturedAt,
                BAR_SOURCE,
                archive.status(),
                archive.itemCount(),
                archive.failureReason()
        );
    }

    private EarlyMarketDataCapture captureMarketSnapshots(
            LocalDate tradeDate,
            List<String> stockCodes,
            EarlyMarketSnapshotType snapshotType,
            EarlyMarketCaptureType captureType,
            Instant capturedAt,
            boolean onlyWhenBarsMissing
    ) {
        StageArchive archive = loadStageArchive(
                tradeDate,
                stockCodes,
                LocalTime.MIN,
                LocalTime.MIN,
                snapshotType,
                capturedAt,
                onlyWhenBarsMissing
        );
        return saveCapture(
                tradeDate,
                captureType,
                capturedAt,
                SNAPSHOT_SOURCE,
                archive.status(),
                archive.itemCount(),
                archive.failureReason()
        );
    }

    private StageArchive loadStageArchive(
            LocalDate tradeDate,
            List<String> stockCodes,
            LocalTime from,
            LocalTime to,
            EarlyMarketSnapshotType snapshotType,
            Instant capturedAt,
            boolean snapshotWhenBarsMissing
    ) {
        if (stockCodes.isEmpty()) {
            return new StageArchive(
                    EarlyMarketCaptureStatus.SKIPPED,
                    0,
                    null
            );
        }
        List<EarlyMarketIntradayBarSnapshot> bars = new ArrayList<>();
        List<EarlyMarketMarketSnapshot> snapshots = new ArrayList<>();
        int represented = 0;
        int failures = 0;
        int missing = 0;
        boolean loadBars = !from.equals(LocalTime.MIN) || !to.equals(LocalTime.MIN);
        for (String stockCode : stockCodes) {
            List<IntradayBar> loadedBars = List.of();
            if (loadBars) {
                try {
                    loadedBars = intradayBarPort.findBars(
                            stockCode,
                            tradeDate,
                            from,
                            to,
                            BarInterval.ONE_MINUTE
                    );
                    loadedBars.forEach(bar -> bars.add(
                            toBarSnapshot(bar, capturedAt)
                    ));
                } catch (RuntimeException exception) {
                    failures++;
                }
            }
            boolean shouldLoadSnapshot = snapshotType != null
                    && (!snapshotWhenBarsMissing || loadedBars.isEmpty());
            boolean snapshotFound = false;
            if (shouldLoadSnapshot) {
                try {
                    Optional<IntradayMarketSnapshot> snapshot =
                            marketSnapshotPort.getSnapshot(stockCode);
                    if (snapshot.isPresent()) {
                        snapshots.add(toMarketSnapshot(
                                tradeDate,
                                snapshot.orElseThrow(),
                                snapshotType
                        ));
                        snapshotFound = true;
                    }
                } catch (RuntimeException exception) {
                    failures++;
                }
            }
            if (!loadedBars.isEmpty() || snapshotFound) {
                represented++;
            } else {
                missing++;
            }
        }
        int saved = 0;
        try {
            saved += barSnapshotPort.upsertBars(bars).size();
            saved += marketSnapshotArchivePort
                    .upsertMarketSnapshots(snapshots)
                    .size();
        } catch (RuntimeException exception) {
            failures++;
        }
        return new StageArchive(
                status(stockCodes.size(), represented, failures + missing),
                saved,
                failureReason(failures, missing)
        );
    }

    private List<String> signalStockCodes(
            LocalDate tradeDate,
            SignalType signalType
    ) {
        try {
            return tradingSignalQueryPort.find(new TradingSignalSearchCriteria(
                            null,
                            tradeDate,
                            EarlyMarketPreOpenScanner.STRATEGY_NAME,
                            signalType,
                            null,
                            null
                    ))
                    .stream()
                    .map(TradingSignalRecord::stockCode)
                    .distinct()
                    .sorted()
                    .toList();
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    private EarlyMarketDataCapture saveCapture(
            LocalDate tradeDate,
            EarlyMarketCaptureType captureType,
            Instant capturedAt,
            String source,
            EarlyMarketCaptureStatus status,
            int itemCount,
            String failureReason
    ) {
        EarlyMarketDataCapture capture = new EarlyMarketDataCapture(
                null,
                tradeDate,
                captureType,
                capturedAt,
                source,
                status,
                itemCount,
                failureReason,
                capturedAt
        );
        try {
            EarlyMarketDataCapture saved = capturePort.save(capture);
            metricsPort.recordEarlyMarketDataCapture(
                    captureType.name(),
                    metricResult(saved.status())
            );
            return saved;
        } catch (RuntimeException exception) {
            metricsPort.recordEarlyMarketDataCapture(
                    captureType.name(),
                    "failure"
            );
            log.atWarn()
                    .addKeyValue("tradeDate", tradeDate)
                    .addKeyValue("captureType", captureType)
                    .addKeyValue("status", EarlyMarketCaptureStatus.FAILED)
                    .addKeyValue("errorType", exception.getClass().getSimpleName())
                    .log("Early market data capture history persistence failed");
            return new EarlyMarketDataCapture(
                    null,
                    tradeDate,
                    captureType,
                    capturedAt,
                    source,
                    EarlyMarketCaptureStatus.FAILED,
                    itemCount,
                    "CAPTURE_HISTORY_PERSISTENCE_FAILED",
                    capturedAt
            );
        }
    }

    private static EarlyMarketAfterHoursSnapshot toAfterHoursSnapshot(
            LocalDate tradeDate,
            LocalDate previousTradingDay,
            AfterHoursQuote quote,
            Instant capturedAt
    ) {
        return new EarlyMarketAfterHoursSnapshot(
                null,
                tradeDate,
                previousTradingDay,
                capturedAt,
                quote.stockCode(),
                quote.afterHoursPrice(),
                quote.afterHoursChangeRate(),
                quote.afterHoursVolume(),
                quote.afterHoursTradingValue(),
                AFTER_HOURS_SOURCE
        );
    }

    private static EarlyMarketIntradayBarSnapshot toBarSnapshot(
            IntradayBar bar,
            Instant capturedAt
    ) {
        return new EarlyMarketIntradayBarSnapshot(
                null,
                bar.tradeDate(),
                bar.stockCode(),
                capturedAt,
                bar.barTime(),
                BarInterval.ONE_MINUTE,
                bar.openPrice(),
                bar.highPrice(),
                bar.lowPrice(),
                bar.closePrice(),
                bar.volume(),
                bar.tradingValue(),
                bar.vwap(),
                BAR_SOURCE
        );
    }

    private static EarlyMarketMarketSnapshot toMarketSnapshot(
            LocalDate tradeDate,
            IntradayMarketSnapshot snapshot,
            EarlyMarketSnapshotType snapshotType
    ) {
        return new EarlyMarketMarketSnapshot(
                null,
                tradeDate,
                snapshot.stockCode(),
                snapshot.snapshotTime(),
                snapshotType,
                snapshot.currentPrice(),
                snapshot.intradayHigh(),
                snapshot.intradayLow(),
                snapshot.accumulatedVolume(),
                snapshot.accumulatedTradingValue(),
                snapshot.vwap(),
                SNAPSHOT_SOURCE
        );
    }

    private static EarlyMarketCaptureStatus status(
            int expected,
            int represented,
            int problems
    ) {
        if (expected == 0) {
            return EarlyMarketCaptureStatus.SKIPPED;
        }
        if (represented >= expected && problems == 0) {
            return EarlyMarketCaptureStatus.SUCCEEDED;
        }
        if (represented > 0) {
            return EarlyMarketCaptureStatus.PARTIAL;
        }
        return EarlyMarketCaptureStatus.FAILED;
    }

    private static EarlyMarketCaptureStatus rankingStatus(
            int itemCount,
            int lookupFailures,
            int totalFailures
    ) {
        if (itemCount == 0 && lookupFailures == 0 && totalFailures == 0) {
            return EarlyMarketCaptureStatus.SKIPPED;
        }
        return status(
                6,
                itemCount == 0 ? 0 : 6 - lookupFailures,
                totalFailures
        );
    }

    private static String failureReason(int failures, int missing) {
        if (failures == 0 && missing == 0) {
            return null;
        }
        return "LOOKUP_OR_PERSIST_FAILURES_" + failures
                + ";MISSING_ITEMS_" + missing;
    }

    private static String metricResult(EarlyMarketCaptureStatus status) {
        return switch (status) {
            case SUCCEEDED, SKIPPED -> "success";
            case PARTIAL -> "partial";
            case FAILED -> "failure";
        };
    }

    private static LocalDate requireDate(LocalDate tradeDate) {
        return Objects.requireNonNull(tradeDate, "tradeDate");
    }

    private static String requireStockCode(String stockCode) {
        if (stockCode == null || stockCode.isBlank()) {
            throw new IllegalArgumentException("stockCode must not be blank");
        }
        return stockCode.trim();
    }

    private interface RankingSupplier {
        List<MarketRankingStock> get();
    }

    private record RankingLoad(
            List<EarlyMarketRankingSnapshot> snapshots,
            List<String> uniqueStockCodes,
            int failures
    ) {
    }

    private record StageArchive(
            EarlyMarketCaptureStatus status,
            int itemCount,
            String failureReason
    ) {
    }
}
