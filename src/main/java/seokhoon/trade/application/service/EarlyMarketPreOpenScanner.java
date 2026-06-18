package seokhoon.trade.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import seokhoon.trade.application.port.in.EarlyMarketCandidate;
import seokhoon.trade.application.port.in.EarlyMarketScanResult;
import seokhoon.trade.application.port.in.ScanEarlyMarketPreOpenUseCase;
import seokhoon.trade.application.port.in.TradingSignalSearchCriteria;
import seokhoon.trade.application.port.out.IndicatorSnapshotPort;
import seokhoon.trade.application.port.out.AfterHoursMarketDataPort;
import seokhoon.trade.application.port.out.MarketCalendarPort;
import seokhoon.trade.application.port.out.MarketRankingPort;
import seokhoon.trade.application.port.out.MarketRankingStock;
import seokhoon.trade.application.port.out.NotificationDeliveryResult;
import seokhoon.trade.application.port.out.NotificationMessage;
import seokhoon.trade.application.port.out.NotificationPort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.application.port.out.TradingSignalPort;
import seokhoon.trade.application.port.out.TradingSignalQueryPort;
import seokhoon.trade.application.port.out.TradingSignalRecord;
import seokhoon.trade.domain.indicator.IndicatorSnapshot;
import seokhoon.trade.domain.market.AfterHoursQuote;
import seokhoon.trade.domain.stock.Market;
import seokhoon.trade.domain.strategy.SignalType;
import seokhoon.trade.domain.strategy.TradingSignal;
import seokhoon.trade.domain.strategy.TradingSignalStatus;
import seokhoon.trade.config.EarlyMarketStrategyProperties;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class EarlyMarketPreOpenScanner implements ScanEarlyMarketPreOpenUseCase {
    public static final String STRATEGY_NAME = "EARLY_MARKET_BREAKOUT";
    private static final Logger log = LoggerFactory.getLogger(EarlyMarketPreOpenScanner.class);

    private static final BigDecimal MAX_NORMAL_CHANGE_RATE = BigDecimal.valueOf(15);
    private static final List<Market> SCAN_MARKETS = List.of(Market.KOSPI, Market.KOSDAQ);

    private final MarketRankingPort marketRankingPort;
    private final IndicatorSnapshotPort indicatorSnapshotPort;
    private final AfterHoursMarketDataPort afterHoursMarketDataPort;
    private final MarketCalendarPort marketCalendarPort;
    private final TradingSignalPort tradingSignalPort;
    private final TradingSignalQueryPort tradingSignalQueryPort;
    private final NotificationPort notificationPort;
    private final OperationalMetricsPort operationalMetricsPort;
    private final EarlyMarketStrategyProperties strategyProperties;
    private final EarningsStrategyAdjustment earningsAdjustment;
    private SupplyDemandStrategyAdjustment supplyDemandAdjustment;
    private final Clock clock;
    private IndicatorStrategyWarmUpSupport warmUpSupport =
            IndicatorStrategyWarmUpSupport.disabled();

    @Autowired
    public EarlyMarketPreOpenScanner(
            MarketRankingPort marketRankingPort,
            IndicatorSnapshotPort indicatorSnapshotPort,
            AfterHoursMarketDataPort afterHoursMarketDataPort,
            MarketCalendarPort marketCalendarPort,
            TradingSignalPort tradingSignalPort,
            TradingSignalQueryPort tradingSignalQueryPort,
            NotificationPort notificationPort,
            OperationalMetricsPort operationalMetricsPort,
            EarlyMarketStrategyProperties strategyProperties,
            EarningsStrategyAdjustment earningsAdjustment,
            SupplyDemandStrategyAdjustment supplyDemandAdjustment,
            IndicatorStrategyWarmUpSupport warmUpSupport
    ) {
        this(
                marketRankingPort,
                indicatorSnapshotPort,
                afterHoursMarketDataPort,
                marketCalendarPort,
                tradingSignalPort,
                tradingSignalQueryPort,
                notificationPort,
                operationalMetricsPort,
                strategyProperties,
                earningsAdjustment,
                Clock.systemUTC()
        );
        this.warmUpSupport = warmUpSupport;
        this.supplyDemandAdjustment = supplyDemandAdjustment;
    }

    EarlyMarketPreOpenScanner(
            MarketRankingPort marketRankingPort,
            IndicatorSnapshotPort indicatorSnapshotPort,
            TradingSignalPort tradingSignalPort,
            TradingSignalQueryPort tradingSignalQueryPort,
            NotificationPort notificationPort,
            Clock clock
    ) {
        this(
                marketRankingPort,
                indicatorSnapshotPort,
                AfterHoursMarketDataPort.empty(),
                EarlyMarketPreOpenScanner::isWeekday,
                tradingSignalPort,
                tradingSignalQueryPort,
                notificationPort,
                OperationalMetricsPort.noop(),
                new EarlyMarketStrategyProperties(),
                null,
                clock
        );
    }

    EarlyMarketPreOpenScanner(
            MarketRankingPort marketRankingPort,
            IndicatorSnapshotPort indicatorSnapshotPort,
            AfterHoursMarketDataPort afterHoursMarketDataPort,
            TradingSignalPort tradingSignalPort,
            TradingSignalQueryPort tradingSignalQueryPort,
            NotificationPort notificationPort,
            OperationalMetricsPort operationalMetricsPort,
            Clock clock
    ) {
        this(
                marketRankingPort,
                indicatorSnapshotPort,
                afterHoursMarketDataPort,
                EarlyMarketPreOpenScanner::isWeekday,
                tradingSignalPort,
                tradingSignalQueryPort,
                notificationPort,
                operationalMetricsPort,
                new EarlyMarketStrategyProperties(),
                null,
                clock
        );
    }

    EarlyMarketPreOpenScanner(
            MarketRankingPort marketRankingPort,
            IndicatorSnapshotPort indicatorSnapshotPort,
            AfterHoursMarketDataPort afterHoursMarketDataPort,
            MarketCalendarPort marketCalendarPort,
            TradingSignalPort tradingSignalPort,
            TradingSignalQueryPort tradingSignalQueryPort,
            NotificationPort notificationPort,
            OperationalMetricsPort operationalMetricsPort,
            Clock clock
    ) {
        this(
                marketRankingPort,
                indicatorSnapshotPort,
                afterHoursMarketDataPort,
                marketCalendarPort,
                tradingSignalPort,
                tradingSignalQueryPort,
                notificationPort,
                operationalMetricsPort,
                new EarlyMarketStrategyProperties(),
                null,
                clock
        );
    }

    EarlyMarketPreOpenScanner(
            MarketRankingPort marketRankingPort,
            IndicatorSnapshotPort indicatorSnapshotPort,
            AfterHoursMarketDataPort afterHoursMarketDataPort,
            MarketCalendarPort marketCalendarPort,
            TradingSignalPort tradingSignalPort,
            TradingSignalQueryPort tradingSignalQueryPort,
            NotificationPort notificationPort,
            OperationalMetricsPort operationalMetricsPort,
            EarlyMarketStrategyProperties strategyProperties,
            Clock clock
    ) {
        this(marketRankingPort, indicatorSnapshotPort, afterHoursMarketDataPort, marketCalendarPort,
                tradingSignalPort, tradingSignalQueryPort, notificationPort, operationalMetricsPort,
                strategyProperties, null, clock);
    }

    EarlyMarketPreOpenScanner(
            MarketRankingPort marketRankingPort,
            IndicatorSnapshotPort indicatorSnapshotPort,
            AfterHoursMarketDataPort afterHoursMarketDataPort,
            MarketCalendarPort marketCalendarPort,
            TradingSignalPort tradingSignalPort,
            TradingSignalQueryPort tradingSignalQueryPort,
            NotificationPort notificationPort,
            OperationalMetricsPort operationalMetricsPort,
            EarlyMarketStrategyProperties strategyProperties,
            EarningsStrategyAdjustment earningsAdjustment,
            Clock clock
    ) {
        this.marketRankingPort = marketRankingPort;
        this.indicatorSnapshotPort = indicatorSnapshotPort;
        this.afterHoursMarketDataPort = afterHoursMarketDataPort;
        this.marketCalendarPort = marketCalendarPort;
        this.tradingSignalPort = tradingSignalPort;
        this.tradingSignalQueryPort = tradingSignalQueryPort;
        this.notificationPort = notificationPort;
        this.operationalMetricsPort = operationalMetricsPort;
        this.strategyProperties = strategyProperties;
        this.earningsAdjustment = earningsAdjustment;
        this.clock = clock;
    }

    @Override
    public EarlyMarketScanResult scan(LocalDate tradeDate, int limit) {
        Objects.requireNonNull(tradeDate, "tradeDate");
        validateLimit(limit);

        Map<String, CandidateSeed> seeds = collectSeeds(limit);
        IndicatorStrategyWarmUpSupport.Session warmUp =
                warmUpSupport.prepare(seeds.keySet(), tradeDate);
        List<ScoredCandidate> selections = seeds.values().stream()
                .map(seed -> score(seed, tradeDate, warmUp))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(ScoredCandidate::score).reversed()
                        .thenComparing(candidate -> candidate.stock().stockCode()))
                .limit(limit)
                .toList();

        selections.forEach(candidate -> tradingSignalPort.save(new TradingSignal(
                STRATEGY_NAME,
                candidate.stock().stockCode(),
                tradeDate,
                SignalType.EARLY_MARKET_PRE_SCAN,
                candidate.score(),
                candidate.reasons()
        )));

        List<EarlyMarketCandidate> candidates = restoreSavedCandidates(tradeDate, selections);
        NotificationDeliveryResult delivery = sendBriefing(tradeDate, candidates);
        return new EarlyMarketScanResult(
                tradeDate,
                seeds.size(),
                candidates.size(),
                delivery.sent(),
                "08:30 장초반 예비 후보 " + candidates.size() + "개",
                candidates
        );
    }

    private Map<String, CandidateSeed> collectSeeds(int limit) {
        Map<String, CandidateSeed> seeds = new LinkedHashMap<>();
        for (Market market : SCAN_MARKETS) {
            marketRankingPort.findTopTradingValueStocks(market, limit)
                    .forEach(stock -> addSeed(seeds, stock, Source.TRADING_VALUE_TOP));
            marketRankingPort.findTopRisingStocks(market, limit)
                    .forEach(stock -> addSeed(seeds, stock, Source.RISING_TOP));
            marketRankingPort.findVolumeSurgeStocks(market, limit)
                    .forEach(stock -> addSeed(seeds, stock, Source.VOLUME_TOP));
        }
        return seeds;
    }

    private static void addSeed(
            Map<String, CandidateSeed> seeds,
            MarketRankingStock stock,
            Source source
    ) {
        seeds.compute(stock.stockCode(), (stockCode, existing) -> {
            CandidateSeed seed = existing == null
                    ? new CandidateSeed(stock, EnumSet.noneOf(Source.class))
                    : existing;
            seed.sources().add(source);
            return seed;
        });
    }

    private ScoredCandidate score(
            CandidateSeed seed,
            LocalDate tradeDate,
            IndicatorStrategyWarmUpSupport.Session warmUp
    ) {
        int score = 30;
        List<String> reasons = new ArrayList<>();
        reasons.add("EARLY_MARKET_PRE_OPEN_08_30");

        if (seed.sources().contains(Source.TRADING_VALUE_TOP)) {
            score += 20;
            reasons.add("TRADING_VALUE_TOP");
        }
        if (seed.sources().contains(Source.RISING_TOP)
                && seed.stock().changeRate().signum() > 0
                && seed.stock().changeRate().compareTo(MAX_NORMAL_CHANGE_RATE) <= 0) {
            score += 15;
            reasons.add("CHANGE_RATE_FAVORABLE");
        } else if (seed.stock().changeRate().compareTo(MAX_NORMAL_CHANGE_RATE) > 0) {
            reasons.add("OVERHEATED_CHANGE_RATE");
        }
        if (seed.sources().contains(Source.VOLUME_TOP)) {
            score += 15;
            reasons.add("VOLUME_TOP");
        }

        latestIndicator(seed.stock().stockCode(), tradeDate).ifPresentOrElse(indicator -> {
            if (indicator.ma5() == null || indicator.ma20() == null) {
                reasons.add("INDICATOR_DATA_INCOMPLETE");
            } else if (seed.stock().currentPrice().compareTo(indicator.ma5()) >= 0
                    && seed.stock().currentPrice().compareTo(indicator.ma20()) >= 0) {
                reasons.add("ABOVE_MA5_AND_MA20");
            } else {
                reasons.add("BELOW_MA5_OR_MA20");
            }
        }, () -> reasons.add("INDICATOR_DATA_UNAVAILABLE"));

        boolean aboveMovingAverages = reasons.contains("ABOVE_MA5_AND_MA20");
        if (aboveMovingAverages) {
            score += 15;
        }
        IndicatorStrategyWarmUpSupport.Assessment assessment =
                warmUp.assess(seed.stock().stockCode(),
                        seed.stock().currentPrice());
        if (assessment.excluded()) {
            return null;
        }
        score += assessment.scoreAdjustment();
        reasons.addAll(assessment.reasons());
        LocalDate afterHoursTradeDate =
                marketCalendarPort.previousTradingDay(tradeDate);
        reasons.add("AFTER_HOURS_TRADE_DATE_" + afterHoursTradeDate);
        AfterHoursScore afterHoursScore = scoreAfterHours(
                seed.stock().stockCode(),
                afterHoursTradeDate
        );
        score += afterHoursScore.scoreAdjustment();
        reasons.addAll(afterHoursScore.reasons());
        EarningsStrategyAdjustment.Assessment earningsAssessment = assessEarnings(seed.stock().stockCode());
        if (earningsAssessment.excluded()) {
            return null;
        }
        score += earningsAssessment.scoreAdjustment();
        reasons.addAll(earningsAssessment.reasons());
        SupplyDemandStrategyAdjustment.Assessment supplyAssessment = assessSupplyDemand(seed.stock().stockCode());
        if (supplyAssessment.excluded()) {
            return null;
        }
        score += supplyAssessment.scoreAdjustment();
        reasons.addAll(supplyAssessment.reasons());
        return new ScoredCandidate(seed.stock(), score, reasons);
    }

    private SupplyDemandStrategyAdjustment.Assessment assessSupplyDemand(String stockCode) {
        return supplyDemandAdjustment == null
                ? new SupplyDemandStrategyAdjustment.Assessment(0, false, List.of())
                : supplyDemandAdjustment.assess(stockCode, "early_market");
    }

    private EarningsStrategyAdjustment.Assessment assessEarnings(String stockCode) {
        if (earningsAdjustment == null) {
            return new EarningsStrategyAdjustment.Assessment(0, false, List.of());
        }
        return earningsAdjustment.assess(stockCode);
    }

    private AfterHoursScore scoreAfterHours(String stockCode, LocalDate afterHoursTradeDate) {
        Optional<AfterHoursQuote> quote;
        try {
            quote = afterHoursMarketDataPort.findByStockCode(stockCode, afterHoursTradeDate);
        } catch (RuntimeException exception) {
            operationalMetricsPort.recordAfterHoursLookup("failure");
            log.atWarn()
                    .addKeyValue("stockCode", stockCode)
                    .addKeyValue("result", "failure")
                    .addKeyValue("errorType", exception.getClass().getSimpleName())
                    .log("After-hours quote lookup failed");
            return new AfterHoursScore(0, List.of("AFTER_HOURS_DATA_UNAVAILABLE"));
        }
        if (quote.isEmpty()) {
            operationalMetricsPort.recordAfterHoursLookup("not_found");
            log.atInfo()
                    .addKeyValue("stockCode", stockCode)
                    .addKeyValue("result", "not_found")
                    .log("After-hours quote lookup completed");
            return new AfterHoursScore(0, List.of("AFTER_HOURS_DATA_UNAVAILABLE"));
        }

        operationalMetricsPort.recordAfterHoursLookup("found");
        log.atInfo()
                .addKeyValue("stockCode", stockCode)
                .addKeyValue("result", "found")
                .log("After-hours quote lookup completed");
        return scoreAfterHoursQuote(quote.orElseThrow());
    }

    private AfterHoursScore scoreAfterHoursQuote(AfterHoursQuote quote) {
        EarlyMarketStrategyProperties.PreOpen properties =
                strategyProperties.getPreOpen();
        int score = 0;
        List<String> reasons = new ArrayList<>();
        BigDecimal changeRate = quote.afterHoursChangeRate();
        if (changeRate.compareTo(properties.getAfterHoursRiseThreshold()) >= 0) {
            score += properties.getAfterHoursRiseScore();
            reasons.add("AFTER_HOURS_CHANGE_RATE_OVER_"
                    + properties.getAfterHoursRiseThreshold()
                    .stripTrailingZeros()
                    .toPlainString()
                    + "PCT");
        }
        if (quote.afterHoursTradingValue().compareTo(
                properties.getAfterHoursTradingValueThreshold()) >= 0) {
            score += properties.getAfterHoursTradingValueScore();
            reasons.add("AFTER_HOURS_TRADING_VALUE_SUFFICIENT");
        }
        if (changeRate.compareTo(properties.getAfterHoursOverheatThreshold()) >= 0) {
            score += properties.getAfterHoursOverheatPenalty();
            reasons.add("AFTER_HOURS_OVERHEATED");
        }
        if (changeRate.compareTo(properties.getAfterHoursFallThreshold()) <= 0) {
            score += properties.getAfterHoursFallPenalty();
            reasons.add("AFTER_HOURS_DECLINE");
        }
        reasons.add("AFTER_HOURS_SUMMARY_CHANGE_RATE_"
                + changeRate.stripTrailingZeros().toPlainString()
                + "_TRADING_VALUE_"
                + quote.afterHoursTradingValue().stripTrailingZeros().toPlainString());
        return new AfterHoursScore(score, reasons);
    }

    private Optional<IndicatorSnapshot> latestIndicator(
            String stockCode,
            LocalDate tradeDate
    ) {
        return indicatorSnapshotPort.findByStockCodeAndTradeDateBetween(
                        stockCode,
                        tradeDate.minusDays(30),
                        tradeDate
                )
                .stream()
                .max(Comparator.comparing(IndicatorSnapshot::tradeDate));
    }

    private List<EarlyMarketCandidate> restoreSavedCandidates(
            LocalDate tradeDate,
            List<ScoredCandidate> selections
    ) {
        Set<String> selectedCodes = selections.stream()
                .map(candidate -> candidate.stock().stockCode())
                .collect(java.util.stream.Collectors.toSet());
        Map<String, TradingSignalRecord> savedByCode = tradingSignalQueryPort.find(
                        new TradingSignalSearchCriteria(
                                null,
                                tradeDate,
                                STRATEGY_NAME,
                                SignalType.EARLY_MARKET_PRE_SCAN,
                                null,
                                null
                        )
                )
                .stream()
                .filter(record -> selectedCodes.contains(record.stockCode()))
                .collect(java.util.stream.Collectors.toMap(
                        TradingSignalRecord::stockCode,
                        record -> record,
                        (left, right) -> left
                ));
        return selections.stream()
                .map(selection -> toCandidate(savedByCode.get(selection.stock().stockCode()), selection))
                .toList();
    }

    private static EarlyMarketCandidate toCandidate(
            TradingSignalRecord record,
            ScoredCandidate selection
    ) {
        if (record == null) {
            return new EarlyMarketCandidate(
                    null,
                    null,
                    STRATEGY_NAME,
                    selection.stock().stockCode(),
                    selection.score(),
                    selection.reasons(),
                    List.of(),
                    TradingSignalStatus.CREATED
            );
        }
        return new EarlyMarketCandidate(
                null,
                record.id(),
                record.strategyName(),
                record.stockCode(),
                record.score(),
                record.reasons(),
                record.riskReasons(),
                record.status()
        );
    }

    private NotificationDeliveryResult sendBriefing(
            LocalDate tradeDate,
            List<EarlyMarketCandidate> candidates
    ) {
        String body = briefingBody(tradeDate, candidates);
        try {
            return notificationPort.send(new NotificationMessage(
                    "TradeGuard 08:30 장초반 예비 후보 - " + tradeDate,
                    body,
                    clock.instant()
            ));
        } catch (RuntimeException exception) {
            return NotificationDeliveryResult.skipped("notification delivery failed");
        }
    }

    private static String briefingBody(
            LocalDate tradeDate,
            List<EarlyMarketCandidate> candidates
    ) {
        StringBuilder body = new StringBuilder("tradeDate: ")
                .append(tradeDate)
                .append("\n08:30 예비 후보입니다. 주문은 생성하지 않습니다.\n\n");
        if (candidates.isEmpty()) {
            return body.append("- 후보 없음\n").toString();
        }
        candidates.forEach(candidate -> body.append("- signalId=")
                .append(candidate.signalId())
                .append(", stockCode=")
                .append(candidate.stockCode())
                .append(", score=")
                .append(candidate.score())
                .append(", reasons=")
                .append(candidate.reasons())
                .append('\n'));
        return body.toString();
    }

    private static void validateLimit(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be at least 1");
        }
    }

    private static boolean isWeekday(LocalDate date) {
        return date.getDayOfWeek() != DayOfWeek.SATURDAY
                && date.getDayOfWeek() != DayOfWeek.SUNDAY;
    }

    private enum Source {
        TRADING_VALUE_TOP,
        RISING_TOP,
        VOLUME_TOP
    }

    private record CandidateSeed(MarketRankingStock stock, EnumSet<Source> sources) {
    }

    private record ScoredCandidate(MarketRankingStock stock, int score, List<String> reasons) {
    }

    private record AfterHoursScore(int scoreAdjustment, List<String> reasons) {
    }
}
