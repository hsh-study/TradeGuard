package seokhoon.trade.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import seokhoon.trade.application.port.in.EarlyMarketFollowUpCandidate;
import seokhoon.trade.application.port.in.EarlyMarketFollowUpDecision;
import seokhoon.trade.application.port.in.EarlyMarketFollowUpResult;
import seokhoon.trade.application.port.in.FollowUpEarlyMarketCandidatesUseCase;
import seokhoon.trade.application.port.in.LoadEarlyMarketPriceActionFeaturesUseCase;
import seokhoon.trade.application.port.in.TradingSignalSearchCriteria;
import seokhoon.trade.application.port.out.IntradayBarPort;
import seokhoon.trade.application.port.out.IntradayMarketSnapshot;
import seokhoon.trade.application.port.out.MarketSnapshotPort;
import seokhoon.trade.application.port.out.NotificationDeliveryResult;
import seokhoon.trade.application.port.out.NotificationMessage;
import seokhoon.trade.application.port.out.NotificationPort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.application.port.out.TradingSignalQueryPort;
import seokhoon.trade.application.port.out.TradingSignalRecord;
import seokhoon.trade.domain.market.BarInterval;
import seokhoon.trade.domain.market.IntradayBar;
import seokhoon.trade.domain.market.EarlyMarketPriceActionFeatures;
import seokhoon.trade.domain.strategy.SignalType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class EarlyMarketFollowUpService implements FollowUpEarlyMarketCandidatesUseCase {
    private static final Logger log = LoggerFactory.getLogger(EarlyMarketFollowUpService.class);
    private static final LocalTime FOLLOW_UP_FROM = LocalTime.of(9, 5);
    private static final LocalTime FOLLOW_UP_TO = LocalTime.of(9, 20);
    private static final BigDecimal CAUTION_DRAWDOWN = new BigDecimal("-1.0000");
    private static final BigDecimal EXCLUDE_DRAWDOWN = new BigDecimal("-2.0000");
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final TradingSignalQueryPort tradingSignalQueryPort;
    private final IntradayBarPort intradayBarPort;
    private final MarketSnapshotPort marketSnapshotPort;
    private final LoadEarlyMarketPriceActionFeaturesUseCase priceActionFeaturesUseCase;
    private final NotificationPort notificationPort;
    private final OperationalMetricsPort metricsPort;
    private final Clock clock;

    @Autowired
    public EarlyMarketFollowUpService(
            TradingSignalQueryPort tradingSignalQueryPort,
            IntradayBarPort intradayBarPort,
            MarketSnapshotPort marketSnapshotPort,
            LoadEarlyMarketPriceActionFeaturesUseCase priceActionFeaturesUseCase,
            NotificationPort notificationPort,
            OperationalMetricsPort metricsPort
    ) {
        this(
                tradingSignalQueryPort,
                intradayBarPort,
                marketSnapshotPort,
                priceActionFeaturesUseCase,
                notificationPort,
                metricsPort,
                Clock.systemUTC()
        );
    }

    EarlyMarketFollowUpService(
            TradingSignalQueryPort tradingSignalQueryPort,
            IntradayBarPort intradayBarPort,
            MarketSnapshotPort marketSnapshotPort,
            NotificationPort notificationPort,
            OperationalMetricsPort metricsPort,
            Clock clock
    ) {
        this(
                tradingSignalQueryPort,
                intradayBarPort,
                marketSnapshotPort,
                EarlyMarketFollowUpService::insufficientFeatures,
                notificationPort,
                metricsPort,
                clock
        );
    }

    EarlyMarketFollowUpService(
            TradingSignalQueryPort tradingSignalQueryPort,
            IntradayBarPort intradayBarPort,
            MarketSnapshotPort marketSnapshotPort,
            LoadEarlyMarketPriceActionFeaturesUseCase priceActionFeaturesUseCase,
            NotificationPort notificationPort,
            OperationalMetricsPort metricsPort,
            Clock clock
    ) {
        this.tradingSignalQueryPort = tradingSignalQueryPort;
        this.intradayBarPort = intradayBarPort;
        this.marketSnapshotPort = marketSnapshotPort;
        this.priceActionFeaturesUseCase = priceActionFeaturesUseCase;
        this.notificationPort = notificationPort;
        this.metricsPort = metricsPort;
        this.clock = clock;
    }

    @Override
    public EarlyMarketFollowUpResult followUp(LocalDate tradeDate) {
        Objects.requireNonNull(tradeDate, "tradeDate");
        List<TradingSignalRecord> signals = tradingSignalQueryPort.find(
                new TradingSignalSearchCriteria(
                        null,
                        tradeDate,
                        EarlyMarketPreOpenScanner.STRATEGY_NAME,
                        SignalType.EARLY_MARKET_ENTRY_CANDIDATE,
                        null,
                        null
                )
        );
        List<EarlyMarketFollowUpCandidate> candidates = signals.stream()
                .sorted(Comparator.comparing(
                        TradingSignalRecord::id,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .map(this::evaluate)
                .toList();
        candidates.forEach(candidate ->
                metricsPort.recordEarlyMarketFollowUp(candidate.decision().name().toLowerCase())
        );

        int keepCount = count(candidates, EarlyMarketFollowUpDecision.KEEP);
        int cautionCount = count(candidates, EarlyMarketFollowUpDecision.CAUTION);
        int excludeCount = count(candidates, EarlyMarketFollowUpDecision.EXCLUDE);
        EarlyMarketFollowUpResult result = new EarlyMarketFollowUpResult(
                tradeDate,
                candidates.size(),
                keepCount,
                cautionCount,
                excludeCount,
                false,
                List.copyOf(candidates)
        );
        NotificationDeliveryResult delivery = sendBriefing(result);
        EarlyMarketFollowUpResult deliveredResult = new EarlyMarketFollowUpResult(
                tradeDate,
                candidates.size(),
                keepCount,
                cautionCount,
                excludeCount,
                delivery.sent(),
                List.copyOf(candidates)
        );
        log.atInfo()
                .addKeyValue("tradeDate", tradeDate)
                .addKeyValue("checkedCount", candidates.size())
                .addKeyValue("keepCount", keepCount)
                .addKeyValue("cautionCount", cautionCount)
                .addKeyValue("excludeCount", excludeCount)
                .addKeyValue("briefingSent", delivery.sent())
                .log("Early market candidates followed up");
        return deliveredResult;
    }

    private EarlyMarketFollowUpCandidate evaluate(TradingSignalRecord signal) {
        List<IntradayBar> bars = loadBars(signal);
        EarlyMarketFollowUpCandidate candidate = !bars.isEmpty()
                ? evaluateBars(signal, bars)
                : evaluateSnapshot(signal, loadSnapshot(signal.stockCode()));
        return applyPriceAction(candidate, loadPriceActionFeatures(signal));
    }

    private EarlyMarketPriceActionFeatures loadPriceActionFeatures(
            TradingSignalRecord signal
    ) {
        try {
            return priceActionFeaturesUseCase.load(
                    signal.stockCode(),
                    signal.signalDate(),
                    FOLLOW_UP_TO
            );
        } catch (RuntimeException exception) {
            return insufficientFeatures(
                    signal.stockCode(),
                    signal.signalDate(),
                    FOLLOW_UP_TO
            );
        }
    }

    private List<IntradayBar> loadBars(TradingSignalRecord signal) {
        try {
            List<IntradayBar> bars = intradayBarPort.findBars(
                    signal.stockCode(),
                    signal.signalDate(),
                    FOLLOW_UP_FROM,
                    FOLLOW_UP_TO,
                    BarInterval.ONE_MINUTE
            ).stream()
                    .sorted(Comparator.comparing(IntradayBar::barTime))
                    .toList();
            metricsPort.recordIntradayBarLookup(bars.isEmpty() ? "not_found" : "found");
            return bars;
        } catch (RuntimeException exception) {
            metricsPort.recordIntradayBarLookup("failure");
            log.atWarn()
                    .addKeyValue("signalId", signal.id())
                    .addKeyValue("stockCode", signal.stockCode())
                    .addKeyValue("result", "failure")
                    .addKeyValue("errorType", exception.getClass().getSimpleName())
                    .log("Early market follow-up intraday bar lookup failed");
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
                    .log("Early market follow-up snapshot lookup failed");
            return Optional.empty();
        }
    }

    private static EarlyMarketFollowUpCandidate evaluateBars(
            TradingSignalRecord signal,
            List<IntradayBar> bars
    ) {
        BigDecimal lastPrice = bars.getLast().closePrice();
        BigDecimal high = bars.stream()
                .map(IntradayBar::highPrice)
                .max(BigDecimal::compareTo)
                .orElseThrow();
        BigDecimal drawdown = drawdown(lastPrice, high);
        boolean vwapBroken = bars.stream()
                .anyMatch(bar -> bar.closePrice().compareTo(bar.vwap()) < 0);
        boolean lastBelowVwap =
                lastPrice.compareTo(bars.getLast().vwap()) < 0;
        return candidate(
                signal,
                lastPrice,
                high,
                drawdown,
                vwapBroken,
                lastBelowVwap,
                "BARS_USED"
        );
    }

    private static EarlyMarketFollowUpCandidate evaluateSnapshot(
            TradingSignalRecord signal,
            Optional<IntradayMarketSnapshot> snapshot
    ) {
        if (snapshot.isEmpty()
                || snapshot.get().currentPrice() == null
                || snapshot.get().intradayHigh() == null
                || snapshot.get().intradayHigh().signum() <= 0
                || snapshot.get().vwap() == null) {
            return new EarlyMarketFollowUpCandidate(
                    signal.id(),
                    signal.stockCode(),
                    signal.score(),
                    EarlyMarketFollowUpDecision.CAUTION,
                    List.of("SNAPSHOT_PROXY", "DATA_INSUFFICIENT"),
                    snapshot.map(IntradayMarketSnapshot::currentPrice).orElse(null),
                    snapshot.map(IntradayMarketSnapshot::intradayHigh).orElse(null),
                    null,
                    null
            );
        }
        IntradayMarketSnapshot value = snapshot.get();
        boolean belowVwap = value.currentPrice().compareTo(value.vwap()) < 0;
        return candidate(
                signal,
                value.currentPrice(),
                value.intradayHigh(),
                drawdown(value.currentPrice(), value.intradayHigh()),
                belowVwap,
                belowVwap,
                "SNAPSHOT_PROXY"
        );
    }

    private static EarlyMarketFollowUpCandidate candidate(
            TradingSignalRecord signal,
            BigDecimal lastPrice,
            BigDecimal high,
            BigDecimal drawdown,
            boolean vwapBroken,
            boolean lastBelowVwap,
            String dataReason
    ) {
        List<String> reasons = new ArrayList<>();
        reasons.add(dataReason);
        EarlyMarketFollowUpDecision decision;
        if (lastBelowVwap) {
            decision = EarlyMarketFollowUpDecision.EXCLUDE;
            reasons.add("LAST_PRICE_BELOW_VWAP");
        } else if (drawdown.compareTo(EXCLUDE_DRAWDOWN) <= 0) {
            decision = EarlyMarketFollowUpDecision.EXCLUDE;
            reasons.add("DRAWDOWN_FROM_HIGH_AT_LEAST_2_PERCENT");
        } else if (vwapBroken) {
            decision = EarlyMarketFollowUpDecision.CAUTION;
            reasons.add("VWAP_BROKEN_DURING_WINDOW");
        } else if (drawdown.compareTo(CAUTION_DRAWDOWN) <= 0) {
            decision = EarlyMarketFollowUpDecision.CAUTION;
            reasons.add("DRAWDOWN_FROM_HIGH_1_TO_2_PERCENT");
        } else {
            decision = EarlyMarketFollowUpDecision.KEEP;
            reasons.add("VWAP_MAINTAINED");
            reasons.add("HIGH_ZONE_MAINTAINED");
        }
        return new EarlyMarketFollowUpCandidate(
                signal.id(),
                signal.stockCode(),
                signal.score(),
                decision,
                List.copyOf(reasons),
                lastPrice,
                high,
                drawdown,
                vwapBroken
        );
    }

    private static EarlyMarketFollowUpCandidate applyPriceAction(
            EarlyMarketFollowUpCandidate candidate,
            EarlyMarketPriceActionFeatures features
    ) {
        List<String> reasons = new ArrayList<>(candidate.reasons());
        EarlyMarketFollowUpDecision decision = candidate.decision();
        if (!features.dataSufficient()) {
            reasons.add("PRICE_ACTION_DATA_INSUFFICIENT");
            reasons.addAll(features.reasons());
            if (decision == EarlyMarketFollowUpDecision.KEEP) {
                decision = EarlyMarketFollowUpDecision.CAUTION;
            }
        } else if (Boolean.FALSE.equals(features.heldOpeningPrice())) {
            decision = EarlyMarketFollowUpDecision.EXCLUDE;
            reasons.add("OPENING_SUPPORT_FAILED");
            reasons.addAll(features.reasons());
        } else {
            if (Boolean.TRUE.equals(features.pullbackRecovered())) {
                reasons.add("PULLBACK_RECOVERED");
            }
            if (Boolean.TRUE.equals(features.brokePreviousHigh())
                    && features.lastPrice().compareTo(features.previousHigh()) >= 0) {
                reasons.add("PREVIOUS_HIGH_HELD");
            } else if (Boolean.TRUE.equals(features.brokePreviousHigh())) {
                reasons.add("PREVIOUS_HIGH_REENTRY_FAILED");
                if (decision == EarlyMarketFollowUpDecision.KEEP) {
                    decision = EarlyMarketFollowUpDecision.CAUTION;
                }
            } else {
                reasons.add("PREVIOUS_HIGH_NOT_BROKEN");
                if (decision == EarlyMarketFollowUpDecision.KEEP) {
                    decision = EarlyMarketFollowUpDecision.CAUTION;
                }
            }
            features.reasons().stream()
                    .filter(reason -> !reasons.contains(reason))
                    .forEach(reasons::add);
        }
        return new EarlyMarketFollowUpCandidate(
                candidate.signalId(),
                candidate.stockCode(),
                candidate.signalScore(),
                decision,
                List.copyOf(reasons),
                candidate.lastPrice(),
                candidate.highSince0905(),
                candidate.drawdownFromHigh(),
                candidate.vwapBroken()
        );
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

    private NotificationDeliveryResult sendBriefing(EarlyMarketFollowUpResult result) {
        try {
            return notificationPort.send(new NotificationMessage(
                    "TradeGuard 09:20 장초반 follow-up - " + result.tradeDate(),
                    briefingBody(result),
                    clock.instant()
            ));
        } catch (RuntimeException exception) {
            return NotificationDeliveryResult.skipped("notification delivery failed");
        }
    }

    static String briefingBody(EarlyMarketFollowUpResult result) {
        StringBuilder body = new StringBuilder("tradeDate: ")
                .append(result.tradeDate())
                .append("\n09:20 장초반 후보 재평가입니다. 주문은 생성하지 않습니다.\n\n")
                .append("- KEEP: ").append(result.keepCount()).append('\n')
                .append("- CAUTION: ").append(result.cautionCount()).append('\n')
                .append("- EXCLUDE: ").append(result.excludeCount()).append('\n')
                .append("\n상위 KEEP 후보\n");
        List<EarlyMarketFollowUpCandidate> topKeep = result.candidates().stream()
                .filter(candidate -> candidate.decision() == EarlyMarketFollowUpDecision.KEEP)
                .sorted(Comparator.comparingInt(
                        EarlyMarketFollowUpCandidate::signalScore
                ).reversed())
                .limit(3)
                .toList();
        if (topKeep.isEmpty()) {
            body.append("- 없음\n");
        } else {
            topKeep.forEach(candidate -> body.append("- signalId=")
                    .append(candidate.signalId())
                    .append(", stockCode=")
                    .append(candidate.stockCode())
                    .append(", score=")
                    .append(candidate.signalScore())
                    .append(", drawdown=")
                    .append(formatRate(candidate.drawdownFromHigh()))
                    .append("%\n"));
        }
        body.append("\nEXCLUDE 후보\n");
        List<EarlyMarketFollowUpCandidate> excluded = result.candidates().stream()
                .filter(candidate -> candidate.decision() == EarlyMarketFollowUpDecision.EXCLUDE)
                .toList();
        if (excluded.isEmpty()) {
            body.append("- 없음\n");
        } else {
            excluded.forEach(candidate -> body.append("- signalId=")
                    .append(candidate.signalId())
                    .append(", stockCode=")
                    .append(candidate.stockCode())
                    .append(", reasons=")
                    .append(candidate.reasons())
                    .append('\n'));
        }
        return body.toString();
    }

    private static BigDecimal drawdown(BigDecimal lastPrice, BigDecimal high) {
        return lastPrice.subtract(high)
                .multiply(ONE_HUNDRED)
                .divide(high, 4, RoundingMode.HALF_UP);
    }

    private static String formatRate(BigDecimal rate) {
        return rate == null ? "N/A" : rate.stripTrailingZeros().toPlainString();
    }

    private static int count(
            List<EarlyMarketFollowUpCandidate> candidates,
            EarlyMarketFollowUpDecision decision
    ) {
        return (int) candidates.stream()
                .filter(candidate -> candidate.decision() == decision)
                .count();
    }
}
