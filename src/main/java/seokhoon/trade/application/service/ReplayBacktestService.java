package seokhoon.trade.application.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import seokhoon.trade.application.port.in.*;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.domain.market.DailyPrice;
import seokhoon.trade.domain.market.EarlyMarketIntradayBarSnapshot;
import seokhoon.trade.domain.research.*;
import seokhoon.trade.domain.strategy.SignalType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReplayBacktestService implements RunReplayBacktestUseCase {
    private static final String CLOSING_STRATEGY_NAME = "CLOSING_BET";
    private static final String EARLY_STRATEGY_NAME = "EARLY_MARKET_BREAKOUT";
    private static final String MISSING_ENTRY = "MISSING_ENTRY_REFERENCE_PRICE";
    private static final String MISSING_EXIT = "MISSING_EXIT_REFERENCE_PRICE";

    private final ReplayBacktestPort backtestPort;
    private final ReplayBacktestSourcePort sourcePort;
    private final TradingSignalQueryPort signalQueryPort;
    private final OperationalMetricsPort metricsPort;
    private final Clock clock;

    @Autowired
    public ReplayBacktestService(ReplayBacktestPort backtestPort,
                                 ReplayBacktestSourcePort sourcePort,
                                 TradingSignalQueryPort signalQueryPort,
                                 OperationalMetricsPort metricsPort) {
        this(backtestPort, sourcePort, signalQueryPort, metricsPort, Clock.systemUTC());
    }

    ReplayBacktestService(ReplayBacktestPort backtestPort,
                          ReplayBacktestSourcePort sourcePort,
                          TradingSignalQueryPort signalQueryPort,
                          OperationalMetricsPort metricsPort,
                          Clock clock) {
        this.backtestPort = backtestPort; this.sourcePort = sourcePort;
        this.signalQueryPort = signalQueryPort; this.metricsPort = metricsPort; this.clock = clock;
    }

    @Override
    public ReplayBacktestRunView runClosingBet(LocalDate fromDate, LocalDate toDate, int holdingDays) {
        validateDates(fromDate, toDate);
        if (holdingDays < 1) throw new IllegalArgumentException("holdingDays must be at least 1");
        String parameters = "{\"source\":\"STORED_TRADING_SIGNALS\",\"entry\":\"DAILY_CLOSE\",\"holdingDays\":" + holdingDays + "}";
        return execute(ReplayBacktestStrategy.CLOSING_BET, fromDate, toDate, parameters,
                runId -> closingResults(runId, fromDate, toDate, holdingDays));
    }

    @Override
    public ReplayBacktestRunView runEarlyMarket(LocalDate fromDate, LocalDate toDate,
                                                LocalTime entryTime, LocalTime exitTime) {
        validateDates(fromDate, toDate);
        Objects.requireNonNull(entryTime, "entryTime");
        Objects.requireNonNull(exitTime, "exitTime");
        if (!exitTime.isAfter(entryTime)) throw new IllegalArgumentException("exitTime must be after entryTime");
        String parameters = "{\"source\":\"STORED_TRADING_SIGNALS_AND_INTRADAY_BARS\",\"entryTime\":\""
                + entryTime + "\",\"exitTime\":\"" + exitTime + "\"}";
        return execute(ReplayBacktestStrategy.EARLY_MARKET, fromDate, toDate, parameters,
                runId -> earlyResults(runId, fromDate, toDate, entryTime, exitTime));
    }

    @Override
    public ReplayBacktestRunView getRun(long runId) {
        ReplayBacktestRun run = backtestPort.findRun(runId).orElseThrow(() -> new ReplayBacktestNotFoundException(runId));
        return view(run, backtestPort.findResults(runId));
    }

    @Override
    public List<ReplayBacktestResult> getResults(long runId) {
        if (backtestPort.findRun(runId).isEmpty()) throw new ReplayBacktestNotFoundException(runId);
        return backtestPort.findResults(runId);
    }

    private ReplayBacktestRunView execute(ReplayBacktestStrategy strategy, LocalDate from, LocalDate to,
                                          String parameters, Function<Long, List<ReplayBacktestResult>> work) {
        Instant now = clock.instant();
        ReplayBacktestRun run = backtestPort.saveRun(new ReplayBacktestRun(null, strategy, from, to,
                ReplayBacktestStatus.CREATED, parameters, 0, 0, 0, null, null, null, null, now, null));
        run = backtestPort.saveRun(copyStatus(run, ReplayBacktestStatus.RUNNING, null));
        try {
            List<ReplayBacktestResult> results = backtestPort.saveResults(work.apply(run.id()));
            ReplayBacktestRun completed = complete(run, results);
            completed = backtestPort.saveRun(completed);
            String metricResult = results.stream().anyMatch(r -> r.resultStatus() == ReplayBacktestResultStatus.DATA_INSUFFICIENT)
                    ? "insufficient" : "success";
            metricsPort.recordReplayBacktest(strategy.name(), metricResult);
            return view(completed, results);
        } catch (RuntimeException exception) {
            backtestPort.saveRun(copyStatus(run, ReplayBacktestStatus.FAILED, truncate(exception.getMessage())));
            metricsPort.recordReplayBacktest(strategy.name(), "failure");
            throw exception;
        }
    }

    private List<ReplayBacktestResult> closingResults(long runId, LocalDate from, LocalDate to, int holdingDays) {
        List<TradingSignalRecord> signals = signals(from, to, CLOSING_STRATEGY_NAME, SignalType.BUY_CANDIDATE);
        return ranked(signals).stream().map(ranked -> {
            TradingSignalRecord signal = ranked.signal();
            Optional<DailyPrice> entry = sourcePort.findDailyPrice(signal.stockCode(), signal.signalDate());
            Optional<DailyPrice> exit = sourcePort.findNthDailyPriceAfter(signal.stockCode(), signal.signalDate(), holdingDays);
            return result(runId, ReplayBacktestStrategy.CLOSING_BET, ranked.rank(), signal,
                    entry.map(DailyPrice::closePrice).orElse(null), exit.map(DailyPrice::closePrice).orElse(null), holdingDays);
        }).toList();
    }

    private List<ReplayBacktestResult> earlyResults(long runId, LocalDate from, LocalDate to,
                                                    LocalTime entryTime, LocalTime exitTime) {
        List<TradingSignalRecord> signals = signals(from, to, EARLY_STRATEGY_NAME, SignalType.EARLY_MARKET_ENTRY_CANDIDATE);
        return ranked(signals).stream().map(ranked -> {
            TradingSignalRecord signal = ranked.signal();
            List<EarlyMarketIntradayBarSnapshot> bars = sourcePort.findIntradayBars(signal.stockCode(), signal.signalDate());
            BigDecimal entry = priceAtOrBefore(bars, entryTime);
            BigDecimal exit = priceAtOrBefore(bars, exitTime);
            return result(runId, ReplayBacktestStrategy.EARLY_MARKET, ranked.rank(), signal, entry, exit, 0);
        }).toList();
    }

    private List<TradingSignalRecord> signals(LocalDate from, LocalDate to, String strategy, SignalType type) {
        List<TradingSignalRecord> results = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            results.addAll(signalQueryPort.find(new TradingSignalSearchCriteria(null, date, strategy, type, null, null)));
        }
        return results;
    }

    private static List<RankedSignal> ranked(List<TradingSignalRecord> signals) {
        List<RankedSignal> ranked = new ArrayList<>();
        signals.stream().collect(Collectors.groupingBy(TradingSignalRecord::signalDate, TreeMap::new, Collectors.toList()))
                .forEach((date, daily) -> {
                    List<TradingSignalRecord> sorted = daily.stream()
                            .sorted(Comparator.comparingInt(TradingSignalRecord::score).reversed()
                                    .thenComparing(TradingSignalRecord::stockCode)).toList();
                    for (int i = 0; i < sorted.size(); i++) ranked.add(new RankedSignal(i + 1, sorted.get(i)));
                });
        return ranked;
    }

    private ReplayBacktestResult result(long runId, ReplayBacktestStrategy strategy, int rank,
                                        TradingSignalRecord signal, BigDecimal entry, BigDecimal exit, Integer holdingDays) {
        List<String> warnings = new ArrayList<>(signal.riskReasons());
        if (entry == null || entry.signum() <= 0) warnings.add(MISSING_ENTRY);
        if (exit == null) warnings.add(MISSING_EXIT);
        boolean dataInsufficient = warnings.contains(MISSING_ENTRY) || warnings.contains(MISSING_EXIT);
        BigDecimal returnRate = dataInsufficient ? null : calculateReturnRate(entry, exit);
        ReplayBacktestResultStatus status = returnRate == null ? ReplayBacktestResultStatus.DATA_INSUFFICIENT
                : returnRate.signum() > 0 ? ReplayBacktestResultStatus.WIN
                : returnRate.signum() < 0 ? ReplayBacktestResultStatus.LOSS : ReplayBacktestResultStatus.FLAT;
        return new ReplayBacktestResult(null, runId, signal.signalDate(), signal.stockCode(),
                sourcePort.findStockName(signal.stockCode()).orElse(signal.stockCode()), strategy, rank,
                signal.score(), signal.reasons(), List.copyOf(warnings), entry, exit, holdingDays,
                returnRate, status, clock.instant());
    }

    static BigDecimal calculateReturnRate(BigDecimal entry, BigDecimal exit) {
        if (entry == null || entry.signum() <= 0 || exit == null) throw new IllegalArgumentException("valid prices are required");
        return exit.subtract(entry).multiply(BigDecimal.valueOf(100)).divide(entry, 6, RoundingMode.HALF_UP);
    }

    private static BigDecimal priceAtOrBefore(List<EarlyMarketIntradayBarSnapshot> bars, LocalTime time) {
        return bars.stream().filter(bar -> !bar.barTime().isAfter(time))
                .max(Comparator.comparing(EarlyMarketIntradayBarSnapshot::barTime))
                .map(EarlyMarketIntradayBarSnapshot::closePrice).orElse(null);
    }

    private ReplayBacktestRun complete(ReplayBacktestRun run, List<ReplayBacktestResult> results) {
        List<BigDecimal> returns = returns(results);
        int wins = (int) results.stream().filter(r -> r.resultStatus() == ReplayBacktestResultStatus.WIN).count();
        int losses = (int) results.stream().filter(r -> r.resultStatus() == ReplayBacktestResultStatus.LOSS).count();
        return new ReplayBacktestRun(run.id(), run.strategy(), run.fromDate(), run.toDate(), ReplayBacktestStatus.COMPLETED,
                run.parameterSnapshot(), results.size(), wins, losses, average(returns), max(returns), min(returns),
                null, run.createdAt(), clock.instant());
    }

    private ReplayBacktestRun copyStatus(ReplayBacktestRun run, ReplayBacktestStatus status, String failure) {
        return new ReplayBacktestRun(run.id(), run.strategy(), run.fromDate(), run.toDate(), status, run.parameterSnapshot(),
                run.totalCandidates(), run.winCount(), run.lossCount(), run.averageReturnRate(), run.maxReturnRate(),
                run.minReturnRate(), failure, run.createdAt(), status == ReplayBacktestStatus.FAILED ? clock.instant() : null);
    }

    static ReplayBacktestRunView view(ReplayBacktestRun run, List<ReplayBacktestResult> results) {
        List<BigDecimal> returns = returns(results).stream().sorted().toList();
        long evaluated = returns.size();
        BigDecimal winRate = evaluated == 0 ? null : BigDecimal.valueOf(run.winCount() * 100L)
                .divide(BigDecimal.valueOf(evaluated), 6, RoundingMode.HALF_UP);
        return new ReplayBacktestRunView(run, winRate, median(returns), averageScore(results, ReplayBacktestResultStatus.WIN),
                averageScore(results, ReplayBacktestResultStatus.LOSS), breakdown(results, ReplayBacktestResult::reasons),
                breakdown(results, ReplayBacktestResult::warnings));
    }

    private static List<ReplayBacktestBreakdown> breakdown(List<ReplayBacktestResult> results,
                                                            Function<ReplayBacktestResult, List<String>> classifier) {
        Map<String, List<ReplayBacktestResult>> groups = new TreeMap<>();
        results.forEach(result -> classifier.apply(result).forEach(key -> groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(result)));
        return groups.entrySet().stream().map(entry -> {
            List<BigDecimal> values = returns(entry.getValue());
            long wins = entry.getValue().stream().filter(r -> r.resultStatus() == ReplayBacktestResultStatus.WIN).count();
            BigDecimal winRate = values.isEmpty() ? null : BigDecimal.valueOf(wins * 100)
                    .divide(BigDecimal.valueOf(values.size()), 6, RoundingMode.HALF_UP);
            return new ReplayBacktestBreakdown(entry.getKey(), entry.getValue().size(), values.size(), winRate, average(values));
        }).toList();
    }

    private static List<BigDecimal> returns(List<ReplayBacktestResult> results) {
        return results.stream().map(ReplayBacktestResult::returnRate).filter(Objects::nonNull).toList();
    }
    private static BigDecimal average(List<BigDecimal> values) {
        return values.isEmpty() ? null : values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 6, RoundingMode.HALF_UP);
    }
    private static BigDecimal min(List<BigDecimal> values) { return values.stream().min(BigDecimal::compareTo).orElse(null); }
    private static BigDecimal max(List<BigDecimal> values) { return values.stream().max(BigDecimal::compareTo).orElse(null); }
    private static BigDecimal median(List<BigDecimal> sorted) {
        if (sorted.isEmpty()) return null;
        int middle = sorted.size() / 2;
        return sorted.size() % 2 == 1 ? sorted.get(middle)
                : sorted.get(middle - 1).add(sorted.get(middle)).divide(BigDecimal.valueOf(2), 6, RoundingMode.HALF_UP);
    }
    private static BigDecimal averageScore(List<ReplayBacktestResult> results, ReplayBacktestResultStatus status) {
        List<Integer> scores = results.stream().filter(r -> r.resultStatus() == status).map(ReplayBacktestResult::score).toList();
        return scores.isEmpty() ? null : BigDecimal.valueOf(scores.stream().mapToInt(Integer::intValue).sum())
                .divide(BigDecimal.valueOf(scores.size()), 6, RoundingMode.HALF_UP);
    }
    private static void validateDates(LocalDate from, LocalDate to) {
        Objects.requireNonNull(from, "fromDate"); Objects.requireNonNull(to, "toDate");
        if (from.isAfter(to)) throw new IllegalArgumentException("fromDate must be on or before toDate");
    }
    private static String truncate(String value) {
        if (value == null) return "Unknown replay failure";
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
    private record RankedSignal(int rank, TradingSignalRecord signal) { }
}
