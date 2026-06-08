package seokhoon.trade.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import seokhoon.trade.application.port.in.ClosingBetCandidateScanResult;
import seokhoon.trade.application.port.in.ClosingBetPreScanCandidate;
import seokhoon.trade.application.port.in.ScanClosingBetCandidatesUseCase;
import seokhoon.trade.application.port.in.TradingSignalSearchCriteria;
import seokhoon.trade.application.port.out.MarketRankingPort;
import seokhoon.trade.application.port.out.MarketRankingStock;
import seokhoon.trade.application.port.out.NotificationDeliveryResult;
import seokhoon.trade.application.port.out.NotificationMessage;
import seokhoon.trade.application.port.out.NotificationPort;
import seokhoon.trade.application.port.out.TradingSignalPort;
import seokhoon.trade.application.port.out.TradingSignalQueryPort;
import seokhoon.trade.application.port.out.TradingSignalRecord;
import seokhoon.trade.domain.stock.Market;
import seokhoon.trade.domain.strategy.SignalType;
import seokhoon.trade.domain.strategy.TradingSignal;
import seokhoon.trade.domain.strategy.TradingSignalStatus;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class ClosingBetCandidateScanner implements ScanClosingBetCandidatesUseCase {
    public static final String STRATEGY_NAME = "CLOSING_BET_PRE_SCAN";

    private static final BigDecimal MIN_TRADING_VALUE = BigDecimal.valueOf(30_000_000_000L);
    private static final BigDecimal MIN_CHANGE_RATE = BigDecimal.valueOf(3);
    private static final BigDecimal MAX_CHANGE_RATE = BigDecimal.valueOf(15);
    private static final int MIN_SCORE = 70;
    private static final List<Market> SCAN_MARKETS = List.of(Market.KOSPI, Market.KOSDAQ);

    private final MarketRankingPort marketRankingPort;
    private final TradingSignalPort tradingSignalPort;
    private final TradingSignalQueryPort tradingSignalQueryPort;
    private final NotificationPort notificationPort;
    private final Clock clock;

    @Autowired
    public ClosingBetCandidateScanner(
            MarketRankingPort marketRankingPort,
            TradingSignalPort tradingSignalPort,
            TradingSignalQueryPort tradingSignalQueryPort,
            NotificationPort notificationPort
    ) {
        this(marketRankingPort, tradingSignalPort, tradingSignalQueryPort, notificationPort, Clock.systemUTC());
    }

    ClosingBetCandidateScanner(
            MarketRankingPort marketRankingPort,
            TradingSignalPort tradingSignalPort,
            TradingSignalQueryPort tradingSignalQueryPort,
            NotificationPort notificationPort,
            Clock clock
    ) {
        this.marketRankingPort = marketRankingPort;
        this.tradingSignalPort = tradingSignalPort;
        this.tradingSignalQueryPort = tradingSignalQueryPort;
        this.notificationPort = notificationPort;
        this.clock = clock;
    }

    @Override
    public ClosingBetCandidateScanResult scan(LocalDate tradeDate, int limit) {
        Objects.requireNonNull(tradeDate, "tradeDate");
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be at least 1");
        }

        Map<String, CandidateSeed> seeds = collectSeeds(limit);
        List<ScoredCandidate> filtered = seeds.values().stream()
                .filter(seed -> seed.stock().tradingValue().compareTo(MIN_TRADING_VALUE) >= 0)
                .filter(seed -> seed.stock().changeRate().compareTo(MIN_CHANGE_RATE) >= 0)
                .filter(seed -> seed.stock().changeRate().compareTo(MAX_CHANGE_RATE) <= 0)
                .map(this::score)
                .filter(candidate -> candidate.score() >= MIN_SCORE)
                .sorted(Comparator.comparingInt(ScoredCandidate::score).reversed()
                        .thenComparing(candidate -> candidate.stock().stockCode()))
                .limit(limit)
                .toList();

        filtered.forEach(candidate -> tradingSignalPort.save(new TradingSignal(
                STRATEGY_NAME,
                candidate.stock().stockCode(),
                tradeDate,
                SignalType.BUY_CANDIDATE,
                candidate.score(),
                candidate.reasons()
        )));

        List<ClosingBetPreScanCandidate> selectedCandidates = restoreSavedCandidates(tradeDate, filtered);
        NotificationDeliveryResult deliveryResult = sendBriefing(tradeDate, selectedCandidates);
        String summary = "14:00 예비 스캔 후보 " + selectedCandidates.size() + "개";
        return new ClosingBetCandidateScanResult(
                tradeDate,
                seeds.size(),
                filtered.size(),
                selectedCandidates.size(),
                deliveryResult.sent(),
                summary,
                selectedCandidates
        );
    }

    private Map<String, CandidateSeed> collectSeeds(int limit) {
        Map<String, CandidateSeed> seeds = new LinkedHashMap<>();
        for (Market market : SCAN_MARKETS) {
            marketRankingPort.findTopTradingValueStocks(market, limit)
                    .forEach(stock -> addSeed(seeds, stock, CandidateSource.TRADING_VALUE_TOP));
            marketRankingPort.findTopRisingStocks(market, limit)
                    .forEach(stock -> addSeed(seeds, stock, CandidateSource.RISING_TOP));
            marketRankingPort.findVolumeSurgeStocks(market, limit)
                    .forEach(stock -> addSeed(seeds, stock, CandidateSource.VOLUME_SURGE));
        }
        return seeds;
    }

    private static void addSeed(
            Map<String, CandidateSeed> seeds,
            MarketRankingStock stock,
            CandidateSource source
    ) {
        seeds.compute(stock.stockCode(), (stockCode, existing) -> {
            if (existing == null) {
                CandidateSeed seed = new CandidateSeed(stock, EnumSet.noneOf(CandidateSource.class));
                seed.sources().add(source);
                return seed;
            }
            existing.sources().add(source);
            return existing;
        });
    }

    private ScoredCandidate score(CandidateSeed seed) {
        int score = 40;
        List<String> reasons = new ArrayList<>();
        reasons.add("MARKET_SCAN_14_00");

        if (seed.sources().contains(CandidateSource.TRADING_VALUE_TOP)) {
            score += 20;
            reasons.add("TRADING_VALUE_TOP");
        }
        if (seed.sources().contains(CandidateSource.RISING_TOP)) {
            score += 15;
            reasons.add("RISING_TOP");
        }
        if (seed.sources().contains(CandidateSource.VOLUME_SURGE)) {
            score += 15;
            reasons.add("VOLUME_SURGE");
        }
        if (seed.stock().tradingValue().compareTo(BigDecimal.valueOf(50_000_000_000L)) >= 0) {
            score += 10;
            reasons.add("TRADING_VALUE_OVER_50B_KRW");
        }
        if (seed.stock().changeRate().compareTo(BigDecimal.valueOf(5)) >= 0) {
            score += 10;
            reasons.add("CHANGE_RATE_OVER_5PCT");
        }
        // TODO: Add VWAP, intraday high location, MA5/MA20 checks when intraday and daily data are available.
        return new ScoredCandidate(seed.stock(), score, reasons);
    }

    private List<ClosingBetPreScanCandidate> restoreSavedCandidates(
            LocalDate tradeDate,
            List<ScoredCandidate> selected
    ) {
        Set<String> selectedCodes = selected.stream()
                .map(candidate -> candidate.stock().stockCode())
                .collect(java.util.stream.Collectors.toSet());
        Map<String, TradingSignalRecord> recordsByCode = tradingSignalQueryPort.find(new TradingSignalSearchCriteria(
                        null,
                        tradeDate,
                        STRATEGY_NAME,
                        SignalType.BUY_CANDIDATE,
                        null,
                        MIN_SCORE
                ))
                .stream()
                .filter(record -> selectedCodes.contains(record.stockCode()))
                .collect(java.util.stream.Collectors.toMap(
                        TradingSignalRecord::stockCode,
                        record -> record,
                        (left, right) -> left
                ));
        return selected.stream()
                .map(candidate -> {
                    TradingSignalRecord record = recordsByCode.get(candidate.stock().stockCode());
                    if (record == null) {
                        return new ClosingBetPreScanCandidate(
                                null,
                                STRATEGY_NAME,
                                candidate.stock().stockCode(),
                                candidate.score(),
                                candidate.reasons(),
                                List.of(),
                                TradingSignalStatus.CREATED
                        );
                    }
                    return new ClosingBetPreScanCandidate(
                            record.id(),
                            record.strategyName(),
                            record.stockCode(),
                            record.score(),
                            record.reasons(),
                            record.riskReasons(),
                            record.status()
                    );
                })
                .toList();
    }

    private NotificationDeliveryResult sendBriefing(
            LocalDate tradeDate,
            List<ClosingBetPreScanCandidate> candidates
    ) {
        StringBuilder body = new StringBuilder();
        body.append("tradeDate: ").append(tradeDate).append('\n');
        body.append("14:00 예비 후보 스캔 결과입니다. 주문 요청은 생성하지 않습니다.\n\n");
        if (candidates.isEmpty()) {
            body.append("- 선정 후보 없음\n");
        } else {
            candidates.forEach(candidate -> body.append("- signalId=")
                    .append(candidate.signalId())
                    .append(", stockCode=")
                    .append(candidate.stockCode())
                    .append(", score=")
                    .append(candidate.score())
                    .append(", reasons=")
                    .append(candidate.reasons())
                    .append('\n'));
        }
        try {
            return notificationPort.send(new NotificationMessage(
                    "TradeGuard 14:00 종가베팅 예비 후보 - " + tradeDate,
                    body.toString(),
                    clock.instant()
            ));
        } catch (RuntimeException exception) {
            return NotificationDeliveryResult.skipped("notification delivery failed");
        }
    }

    private enum CandidateSource {
        TRADING_VALUE_TOP,
        RISING_TOP,
        VOLUME_SURGE
    }

    private record CandidateSeed(MarketRankingStock stock, EnumSet<CandidateSource> sources) {
    }

    private record ScoredCandidate(MarketRankingStock stock, int score, List<String> reasons) {
    }
}
