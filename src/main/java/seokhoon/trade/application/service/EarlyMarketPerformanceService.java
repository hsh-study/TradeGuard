package seokhoon.trade.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import seokhoon.trade.application.port.in.CaptureEarlyMarketPerformancesUseCase;
import seokhoon.trade.application.port.in.EarlyMarketPerformanceCaptureResult;
import seokhoon.trade.application.port.in.EarlyMarketPerformanceView;
import seokhoon.trade.application.port.in.LoadEarlyMarketPerformancesUseCase;
import seokhoon.trade.application.port.in.TradingSignalSearchCriteria;
import seokhoon.trade.application.port.out.EarlyMarketPerformancePort;
import seokhoon.trade.application.port.out.IntradayBarPort;
import seokhoon.trade.application.port.out.IntradayMarketSnapshot;
import seokhoon.trade.application.port.out.MarketSnapshotPort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.application.port.out.TradingSignalQueryPort;
import seokhoon.trade.application.port.out.TradingSignalRecord;
import seokhoon.trade.domain.market.BarInterval;
import seokhoon.trade.domain.market.EarlyMarketCandidatePerformance;
import seokhoon.trade.domain.market.IntradayBar;
import seokhoon.trade.domain.strategy.SignalType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EarlyMarketPerformanceService
        implements CaptureEarlyMarketPerformancesUseCase, LoadEarlyMarketPerformancesUseCase {
    private static final Logger log =
            LoggerFactory.getLogger(EarlyMarketPerformanceService.class);
    private static final LocalTime PERFORMANCE_FROM = LocalTime.of(9, 0);
    private static final LocalTime PERFORMANCE_TO = LocalTime.of(9, 30);
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final TradingSignalQueryPort tradingSignalQueryPort;
    private final IntradayBarPort intradayBarPort;
    private final MarketSnapshotPort marketSnapshotPort;
    private final EarlyMarketPerformancePort performancePort;
    private final OperationalMetricsPort metricsPort;
    private final Clock clock;

    @Autowired
    public EarlyMarketPerformanceService(
            TradingSignalQueryPort tradingSignalQueryPort,
            IntradayBarPort intradayBarPort,
            MarketSnapshotPort marketSnapshotPort,
            EarlyMarketPerformancePort performancePort,
            OperationalMetricsPort metricsPort
    ) {
        this(
                tradingSignalQueryPort,
                intradayBarPort,
                marketSnapshotPort,
                performancePort,
                metricsPort,
                Clock.systemUTC()
        );
    }

    EarlyMarketPerformanceService(
            TradingSignalQueryPort tradingSignalQueryPort,
            IntradayBarPort intradayBarPort,
            MarketSnapshotPort marketSnapshotPort,
            EarlyMarketPerformancePort performancePort,
            OperationalMetricsPort metricsPort,
            Clock clock
    ) {
        this.tradingSignalQueryPort = tradingSignalQueryPort;
        this.intradayBarPort = intradayBarPort;
        this.marketSnapshotPort = marketSnapshotPort;
        this.performancePort = performancePort;
        this.metricsPort = metricsPort;
        this.clock = clock;
    }

    @Override
    public EarlyMarketPerformanceCaptureResult capture(LocalDate tradeDate) {
        Objects.requireNonNull(tradeDate, "tradeDate");
        List<TradingSignalRecord> signals = loadCandidateSignals(tradeDate);
        List<EarlyMarketPerformanceView> captured = new ArrayList<>();

        for (TradingSignalRecord signal : signals) {
            capture(signal).ifPresent(captured::add);
        }

        log.atInfo()
                .addKeyValue("tradeDate", tradeDate)
                .addKeyValue("signalCount", signals.size())
                .addKeyValue("capturedCount", captured.size())
                .log("Early market candidate performances captured");
        return new EarlyMarketPerformanceCaptureResult(
                tradeDate,
                signals.size(),
                captured.size(),
                List.copyOf(captured)
        );
    }

    @Override
    public List<EarlyMarketPerformanceView> findByTradeDate(LocalDate tradeDate) {
        Objects.requireNonNull(tradeDate, "tradeDate");
        Map<Long, TradingSignalRecord> signalsById = loadCandidateSignals(tradeDate)
                .stream()
                .filter(signal -> signal.id() != null)
                .collect(Collectors.toMap(
                        TradingSignalRecord::id,
                        Function.identity(),
                        (left, right) -> left
                ));
        return performancePort.findByTradeDate(tradeDate)
                .stream()
                .map(performance -> toView(
                        performance,
                        Optional.ofNullable(signalsById.get(performance.signalId()))
                                .map(TradingSignalRecord::score)
                                .orElse(0)
                ))
                .toList();
    }

    @Override
    public EarlyMarketPerformanceView findBySignalId(long signalId) {
        if (signalId < 1) {
            throw new IllegalArgumentException("signalId must be at least 1");
        }
        EarlyMarketCandidatePerformance performance = performancePort
                .findBySignalId(signalId)
                .orElseThrow(() -> new EarlyMarketPerformanceNotFoundException(signalId));
        return toView(performance, signalScore(performance));
    }

    private Optional<EarlyMarketPerformanceView> capture(TradingSignalRecord signal) {
        if (signal.id() == null) {
            metricsPort.recordEarlyMarketPerformanceCapture("failed");
            return Optional.empty();
        }

        List<IntradayBar> bars = loadBars(signal);
        boolean barsUsed = !bars.isEmpty();
        Optional<IntradayMarketSnapshot> snapshot = barsUsed
                ? Optional.empty()
                : loadSnapshot(signal.stockCode());
        EarlyMarketCandidatePerformance performance = barsUsed
                ? toPerformance(signal, bars)
                : toPerformance(signal, snapshot);
        try {
            EarlyMarketCandidatePerformance saved = performancePort.save(performance);
            metricsPort.recordEarlyMarketPerformanceCapture(
                    barsUsed ? "bars_used" : "snapshot_proxy"
            );
            log.atInfo()
                    .addKeyValue("signalId", signal.id())
                    .addKeyValue("stockCode", signal.stockCode())
                    .addKeyValue("result", barsUsed ? "bars_used" : "snapshot_proxy")
                    .addKeyValue("barCount", bars.size())
                    .log("Early market candidate performance captured");
            return Optional.of(toView(saved, signal.score()));
        } catch (RuntimeException exception) {
            metricsPort.recordEarlyMarketPerformanceCapture("failed");
            log.atWarn()
                    .addKeyValue("signalId", signal.id())
                    .addKeyValue("stockCode", signal.stockCode())
                    .addKeyValue("result", "failed")
                    .addKeyValue("errorType", exception.getClass().getSimpleName())
                    .log("Early market candidate performance persistence failed");
            return Optional.empty();
        }
    }

    private List<IntradayBar> loadBars(TradingSignalRecord signal) {
        try {
            List<IntradayBar> bars = intradayBarPort.findBars(
                    signal.stockCode(),
                    signal.signalDate(),
                    PERFORMANCE_FROM,
                    PERFORMANCE_TO,
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
                    .addKeyValue("signalId", signal.id())
                    .addKeyValue("stockCode", signal.stockCode())
                    .addKeyValue("result", "failure")
                    .addKeyValue("errorType", exception.getClass().getSimpleName())
                    .log("Early market intraday bar lookup failed");
            return List.of();
        }
    }

    private Optional<IntradayMarketSnapshot> loadSnapshot(String stockCode) {
        try {
            return marketSnapshotPort.getSnapshot(stockCode);
        } catch (RuntimeException exception) {
            log.atWarn()
                    .addKeyValue("stockCode", stockCode)
                    .addKeyValue("result", "snapshot_unavailable")
                    .addKeyValue("errorType", exception.getClass().getSimpleName())
                    .log("Early market performance snapshot lookup failed");
            return Optional.empty();
        }
    }

    private EarlyMarketCandidatePerformance toPerformance(
            TradingSignalRecord signal,
            List<IntradayBar> bars
    ) {
        IntradayBar firstBar = bars.getFirst();
        IntradayBar lastBar = bars.getLast();
        BigDecimal entryReferencePrice = firstBar.openPrice();
        BigDecimal highUntil0930 = bars.stream()
                .map(IntradayBar::highPrice)
                .max(BigDecimal::compareTo)
                .orElseThrow();
        BigDecimal lowUntil0930 = bars.stream()
                .map(IntradayBar::lowPrice)
                .min(BigDecimal::compareTo)
                .orElseThrow();
        boolean vwapBroken = bars.stream()
                .anyMatch(EarlyMarketPerformanceService::isVwapBroken);
        return new EarlyMarketCandidatePerformance(
                signal.id(),
                signal.stockCode(),
                signal.signalDate(),
                signal.signalType(),
                entryReferencePrice,
                highUntil0930,
                lowUntil0930,
                lastBar.closePrice(),
                rateFrom(entryReferencePrice, highUntil0930),
                rateFrom(entryReferencePrice, lowUntil0930),
                vwapBroken,
                clock.instant()
        );
    }

    private EarlyMarketCandidatePerformance toPerformance(
            TradingSignalRecord signal,
            Optional<IntradayMarketSnapshot> snapshot
    ) {
        BigDecimal priceAt0930 = snapshot
                .map(IntradayMarketSnapshot::currentPrice)
                .orElse(null);
        Boolean vwapBroken = snapshot
                .map(EarlyMarketPerformanceService::isVwapBroken)
                .orElse(null);
        return new EarlyMarketCandidatePerformance(
                signal.id(),
                signal.stockCode(),
                signal.signalDate(),
                signal.signalType(),
                null,
                null,
                null,
                priceAt0930,
                null,
                null,
                vwapBroken,
                clock.instant()
        );
    }

    private static BigDecimal rateFrom(BigDecimal referencePrice, BigDecimal price) {
        return price.subtract(referencePrice)
                .multiply(ONE_HUNDRED)
                .divide(referencePrice, 4, RoundingMode.HALF_UP);
    }

    private static boolean isVwapBroken(IntradayBar bar) {
        return bar.closePrice().compareTo(bar.vwap()) < 0;
    }

    private static Boolean isVwapBroken(IntradayMarketSnapshot snapshot) {
        if (snapshot.currentPrice() == null || snapshot.vwap() == null) {
            return null;
        }
        return snapshot.currentPrice().compareTo(snapshot.vwap()) < 0;
    }

    private List<TradingSignalRecord> loadCandidateSignals(LocalDate tradeDate) {
        List<TradingSignalRecord> signals = new ArrayList<>();
        signals.addAll(loadSignals(tradeDate, SignalType.EARLY_MARKET_PRE_SCAN));
        signals.addAll(loadSignals(tradeDate, SignalType.EARLY_MARKET_ENTRY_CANDIDATE));
        return signals.stream()
                .sorted(Comparator.comparing(
                        TradingSignalRecord::id,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .toList();
    }

    private List<TradingSignalRecord> loadSignals(
            LocalDate tradeDate,
            SignalType signalType
    ) {
        return tradingSignalQueryPort.find(new TradingSignalSearchCriteria(
                null,
                tradeDate,
                EarlyMarketPreOpenScanner.STRATEGY_NAME,
                signalType,
                null,
                null
        ));
    }

    private int signalScore(EarlyMarketCandidatePerformance performance) {
        return loadSignals(performance.tradeDate(), performance.signalType())
                .stream()
                .filter(signal -> signal.id() != null
                        && signal.id() == performance.signalId())
                .findFirst()
                .map(TradingSignalRecord::score)
                .orElse(0);
    }

    private static EarlyMarketPerformanceView toView(
            EarlyMarketCandidatePerformance performance,
            int signalScore
    ) {
        return new EarlyMarketPerformanceView(
                performance.signalId(),
                performance.stockCode(),
                performance.tradeDate(),
                performance.signalType(),
                signalScore,
                performance.entryReferencePrice(),
                performance.highUntil0930(),
                performance.lowUntil0930(),
                performance.priceAt0930(),
                performance.maxReturnRateUntil0930(),
                performance.maxDrawdownRateUntil0930(),
                performance.vwapBroken(),
                performance.capturedAt()
        );
    }
}
