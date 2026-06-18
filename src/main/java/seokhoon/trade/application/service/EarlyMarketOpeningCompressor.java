package seokhoon.trade.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import seokhoon.trade.application.port.in.CompressEarlyMarketOpeningUseCase;
import seokhoon.trade.application.port.in.EarlyMarketCandidate;
import seokhoon.trade.application.port.in.LoadEarlyMarketPriceActionFeaturesUseCase;
import seokhoon.trade.application.port.in.EarlyMarketScanResult;
import seokhoon.trade.application.port.in.TradingSignalSearchCriteria;
import seokhoon.trade.application.port.out.IntradayMarketSnapshot;
import seokhoon.trade.application.port.out.MarketSnapshotPort;
import seokhoon.trade.application.port.out.NotificationDeliveryResult;
import seokhoon.trade.application.port.out.NotificationMessage;
import seokhoon.trade.application.port.out.NotificationPort;
import seokhoon.trade.application.port.out.TradingSignalPort;
import seokhoon.trade.application.port.out.TradingSignalQueryPort;
import seokhoon.trade.application.port.out.TradingSignalRecord;
import seokhoon.trade.domain.strategy.SignalType;
import seokhoon.trade.domain.strategy.TradingSignal;
import seokhoon.trade.domain.strategy.TradingSignalStatus;
import seokhoon.trade.domain.market.EarlyMarketPriceActionFeatures;
import seokhoon.trade.config.EarlyMarketStrategyProperties;

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
import java.util.Set;

@Service
public class EarlyMarketOpeningCompressor implements CompressEarlyMarketOpeningUseCase {
    private static final int BASE_SCORE = 40;
    private static final BigDecimal HIGH_ZONE_RATIO = BigDecimal.valueOf(0.95);
    private static final BigDecimal LARGE_PULLBACK_RATIO = BigDecimal.valueOf(0.90);
    private static final BigDecimal MIN_TRADING_VALUE = BigDecimal.valueOf(30_000_000_000L);

    private final TradingSignalQueryPort tradingSignalQueryPort;
    private final TradingSignalPort tradingSignalPort;
    private final MarketSnapshotPort marketSnapshotPort;
    private final LoadEarlyMarketPriceActionFeaturesUseCase priceActionFeaturesUseCase;
    private final NotificationPort notificationPort;
    private final EarlyMarketStrategyProperties strategyProperties;
    private SupplyDemandStrategyAdjustment supplyDemandAdjustment;
    private final Clock clock;
    private IndicatorStrategyWarmUpSupport warmUpSupport =
            IndicatorStrategyWarmUpSupport.disabled();

    @Autowired
    public EarlyMarketOpeningCompressor(
            TradingSignalQueryPort tradingSignalQueryPort,
            TradingSignalPort tradingSignalPort,
            MarketSnapshotPort marketSnapshotPort,
            LoadEarlyMarketPriceActionFeaturesUseCase priceActionFeaturesUseCase,
            NotificationPort notificationPort,
            EarlyMarketStrategyProperties strategyProperties,
            SupplyDemandStrategyAdjustment supplyDemandAdjustment,
            IndicatorStrategyWarmUpSupport warmUpSupport
    ) {
        this(
                tradingSignalQueryPort,
                tradingSignalPort,
                marketSnapshotPort,
                priceActionFeaturesUseCase,
                notificationPort,
                strategyProperties,
                Clock.systemUTC()
        );
        this.warmUpSupport = warmUpSupport;
        this.supplyDemandAdjustment = supplyDemandAdjustment;
    }

    EarlyMarketOpeningCompressor(
            TradingSignalQueryPort tradingSignalQueryPort,
            TradingSignalPort tradingSignalPort,
            MarketSnapshotPort marketSnapshotPort,
            NotificationPort notificationPort,
            Clock clock
    ) {
        this(
                tradingSignalQueryPort,
                tradingSignalPort,
                marketSnapshotPort,
                EarlyMarketOpeningCompressor::insufficientFeatures,
                notificationPort,
                new EarlyMarketStrategyProperties(),
                clock
        );
    }

    EarlyMarketOpeningCompressor(
            TradingSignalQueryPort tradingSignalQueryPort,
            TradingSignalPort tradingSignalPort,
            MarketSnapshotPort marketSnapshotPort,
            LoadEarlyMarketPriceActionFeaturesUseCase priceActionFeaturesUseCase,
            NotificationPort notificationPort,
            Clock clock
    ) {
        this(
                tradingSignalQueryPort,
                tradingSignalPort,
                marketSnapshotPort,
                priceActionFeaturesUseCase,
                notificationPort,
                new EarlyMarketStrategyProperties(),
                clock
        );
    }

    EarlyMarketOpeningCompressor(
            TradingSignalQueryPort tradingSignalQueryPort,
            TradingSignalPort tradingSignalPort,
            MarketSnapshotPort marketSnapshotPort,
            LoadEarlyMarketPriceActionFeaturesUseCase priceActionFeaturesUseCase,
            NotificationPort notificationPort,
            EarlyMarketStrategyProperties strategyProperties,
            Clock clock
    ) {
        this.tradingSignalQueryPort = tradingSignalQueryPort;
        this.tradingSignalPort = tradingSignalPort;
        this.marketSnapshotPort = marketSnapshotPort;
        this.priceActionFeaturesUseCase = priceActionFeaturesUseCase;
        this.notificationPort = notificationPort;
        this.strategyProperties = strategyProperties;
        this.clock = clock;
    }

    @Override
    public EarlyMarketScanResult compress(LocalDate tradeDate, int limit) {
        Objects.requireNonNull(tradeDate, "tradeDate");
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be at least 1");
        }

        List<TradingSignalRecord> preScanSignals = tradingSignalQueryPort.find(
                new TradingSignalSearchCriteria(
                        null,
                        tradeDate,
                        EarlyMarketPreOpenScanner.STRATEGY_NAME,
                        SignalType.EARLY_MARKET_PRE_SCAN,
                        null,
                        null
                )
        );
        IndicatorStrategyWarmUpSupport.Session warmUp =
                warmUpSupport.prepare(
                preScanSignals.stream()
                        .map(TradingSignalRecord::stockCode)
                        .toList(),
                tradeDate
        );
        List<Selection> selections = preScanSignals.stream()
                .filter(signal -> signal.riskReasons().isEmpty())
                .map(signal -> withSnapshot(signal, warmUp))
                .flatMap(java.util.Optional::stream)
                .filter(selection -> selection.score()
                        >= strategyProperties.getOpening().getEntryThreshold())
                .sorted(Comparator.comparingInt(Selection::score).reversed()
                        .thenComparing(selection -> selection.preScan().stockCode()))
                .limit(Math.min(
                        limit,
                        strategyProperties.getOpening().getMaxCandidates()
                ))
                .toList();

        selections.forEach(selection -> tradingSignalPort.save(new TradingSignal(
                EarlyMarketPreOpenScanner.STRATEGY_NAME,
                selection.preScan().stockCode(),
                tradeDate,
                SignalType.EARLY_MARKET_ENTRY_CANDIDATE,
                selection.score(),
                selection.reasons()
        )));

        List<EarlyMarketCandidate> candidates = restoreSavedCandidates(tradeDate, selections);
        NotificationDeliveryResult delivery = sendBriefing(tradeDate, candidates);
        return new EarlyMarketScanResult(
                tradeDate,
                preScanSignals.size(),
                candidates.size(),
                delivery.sent(),
                "09:05 장초반 압축 후보 " + candidates.size() + "개",
                candidates
        );
    }

    private java.util.Optional<Selection> withSnapshot(
            TradingSignalRecord preScan,
            IndicatorStrategyWarmUpSupport.Session warmUp
    ) {
        try {
            return marketSnapshotPort.getSnapshot(preScan.stockCode())
                    .map(snapshot -> score(
                            preScan,
                            snapshot,
                            loadPriceActionFeatures(preScan),
                            warmUp
                    ));
        } catch (RuntimeException exception) {
            return java.util.Optional.empty();
        }
    }

    private EarlyMarketPriceActionFeatures loadPriceActionFeatures(
            TradingSignalRecord preScan
    ) {
        try {
            return priceActionFeaturesUseCase.load(
                    preScan.stockCode(),
                    preScan.signalDate(),
                    LocalTime.of(9, 5)
            );
        } catch (RuntimeException exception) {
            return insufficientFeatures(
                    preScan.stockCode(),
                    preScan.signalDate(),
                    LocalTime.of(9, 5)
            );
        }
    }

    private Selection score(
        TradingSignalRecord preScan,
        IntradayMarketSnapshot snapshot,
        EarlyMarketPriceActionFeatures features,
        IndicatorStrategyWarmUpSupport.Session warmUp
    ) {
        EarlyMarketStrategyProperties.Opening opening =
                strategyProperties.getOpening();
        EarlyMarketStrategyProperties.PriceAction priceAction =
                strategyProperties.getPriceAction();
        int score = BASE_SCORE;
        List<String> reasons = new ArrayList<>();
        reasons.add("EARLY_MARKET_OPENING_09_05");
        reasons.add("PRE_OPEN_CANDIDATE_CONFIRMED");

        if (snapshot.vwap() == null) {
            reasons.add("VWAP_UNAVAILABLE");
        } else if (snapshot.currentPrice().compareTo(snapshot.vwap()) >= 0) {
            score += opening.getVwapAboveScore();
            reasons.add("ABOVE_VWAP");
        } else {
            score += opening.getVwapBrokenPenalty();
            reasons.add("BELOW_VWAP");
        }

        BigDecimal highRatio = highRatio(snapshot);
        if (highRatio.compareTo(HIGH_ZONE_RATIO) >= 0) {
            score += opening.getNearHighScore();
            reasons.add("NEAR_INTRADAY_HIGH");
        } else if (highRatio.compareTo(LARGE_PULLBACK_RATIO) <= 0) {
            score += opening.getHighDrawdownPenalty();
            reasons.add("PULLED_BACK_FROM_INTRADAY_HIGH");
        }

        if (snapshot.accumulatedTradingValue().compareTo(MIN_TRADING_VALUE) >= 0) {
            score += opening.getTradingValueScore();
            reasons.add("ACCUMULATED_TRADING_VALUE_SUFFICIENT");
        } else {
            reasons.add("ACCUMULATED_TRADING_VALUE_INSUFFICIENT");
        }
        if (!features.dataSufficient()) {
            reasons.add("PRICE_ACTION_DATA_INSUFFICIENT");
            reasons.addAll(features.reasons());
        } else {
            if (Boolean.TRUE.equals(features.brokePreviousHigh())) {
                score += priceAction.getPreviousHighBreakoutScore();
            } else {
                score += priceAction.getPreviousHighNotBrokenPenalty();
            }
            if (Boolean.TRUE.equals(features.heldOpeningPrice())) {
                score += priceAction.getOpeningPriceHeldScore();
            } else {
                score += priceAction.getOpeningPriceLostPenalty();
            }
            reasons.addAll(features.reasons());
        }
        IndicatorStrategyWarmUpSupport.Assessment assessment =
                warmUp.assess(preScan.stockCode(),
                        snapshot.currentPrice());
        if (assessment.excluded()) {
            return null;
        }
        score += assessment.scoreAdjustment();
        reasons.addAll(assessment.reasons());
        if (supplyDemandAdjustment != null) {
            SupplyDemandStrategyAdjustment.Assessment supply =
                    supplyDemandAdjustment.assess(preScan.stockCode(), "early_market");
            if (supply.excluded()) {
                return null;
            }
            score += supply.scoreAdjustment();
            reasons.addAll(supply.reasons());
        }
        return new Selection(preScan, score, reasons);
    }

    private static EarlyMarketPriceActionFeatures insufficientFeatures(
            String stockCode,
            LocalDate tradeDate,
            LocalTime ignored
    ) {
        return new EarlyMarketPriceActionFeatures(
                stockCode,
                tradeDate,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                List.of("PRICE_ACTION_FEATURE_UNAVAILABLE")
        );
    }

    private static BigDecimal highRatio(IntradayMarketSnapshot snapshot) {
        if (snapshot.intradayHigh().signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return snapshot.currentPrice().divide(
                snapshot.intradayHigh(),
                4,
                RoundingMode.HALF_UP
        );
    }

    private List<EarlyMarketCandidate> restoreSavedCandidates(
            LocalDate tradeDate,
            List<Selection> selections
    ) {
        Set<String> selectedCodes = selections.stream()
                .map(selection -> selection.preScan().stockCode())
                .collect(java.util.stream.Collectors.toSet());
        Map<String, TradingSignalRecord> savedByCode = tradingSignalQueryPort.find(
                        new TradingSignalSearchCriteria(
                                null,
                                tradeDate,
                                EarlyMarketPreOpenScanner.STRATEGY_NAME,
                                SignalType.EARLY_MARKET_ENTRY_CANDIDATE,
                                null,
                                strategyProperties.getOpening().getEntryThreshold()
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
                .map(selection -> {
                    TradingSignalRecord saved = savedByCode.get(selection.preScan().stockCode());
                    if (saved == null) {
                        return new EarlyMarketCandidate(
                                selection.preScan().id(),
                                null,
                                EarlyMarketPreOpenScanner.STRATEGY_NAME,
                                selection.preScan().stockCode(),
                                selection.score(),
                                selection.reasons(),
                                List.of(),
                                TradingSignalStatus.CREATED
                        );
                    }
                    return new EarlyMarketCandidate(
                            selection.preScan().id(),
                            saved.id(),
                            saved.strategyName(),
                            saved.stockCode(),
                            saved.score(),
                            saved.reasons(),
                            saved.riskReasons(),
                            saved.status()
                    );
                })
                .toList();
    }

    private NotificationDeliveryResult sendBriefing(
            LocalDate tradeDate,
            List<EarlyMarketCandidate> candidates
    ) {
        String body = briefingBody(tradeDate, candidates);
        try {
            return notificationPort.send(new NotificationMessage(
                    "TradeGuard 09:05 장초반 압축 후보 - " + tradeDate,
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
                .append("\n09:05 압축 후보입니다. 주문은 생성하지 않습니다.\n\n");
        if (candidates.isEmpty()) {
            return body.append("- 후보 없음\n").toString();
        }
        candidates.forEach(candidate -> body.append("- preScanSignalId=")
                .append(candidate.sourceSignalId())
                .append(", signalId=")
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

    private record Selection(
            TradingSignalRecord preScan,
            int score,
            List<String> reasons
    ) {
    }
}
