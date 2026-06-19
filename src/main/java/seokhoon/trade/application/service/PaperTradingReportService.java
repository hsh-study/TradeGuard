package seokhoon.trade.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import seokhoon.trade.application.port.in.*;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.config.PaperTradingReportProperties;
import seokhoon.trade.domain.market.*;
import seokhoon.trade.domain.research.*;
import seokhoon.trade.domain.stock.Stock;
import seokhoon.trade.domain.strategy.SignalType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PaperTradingReportService implements GeneratePaperTradingReportUseCase {
    private static final String EARLY_STRATEGY = "EARLY_MARKET_BREAKOUT";
    private static final String CLOSING_STRATEGY = "CLOSING_BET";
    private static final String MISSING_ENTRY = "MISSING_REFERENCE_ENTRY_PRICE";
    private static final String MISSING_EXIT = "MISSING_REFERENCE_EXIT_PRICE";

    private final PaperTradingReportPort reportPort;
    private final ReplayBacktestSourcePort sourcePort;
    private final StockPort stockPort;
    private final TradingSignalQueryPort signalPort;
    private final EarlyMarketPerformancePort performancePort;
    private final EarlyMarketFollowUpResultPort followUpPort;
    private final PaperTradingReportProperties properties;
    private final OperationalMetricsPort metrics;
    private final Clock clock;

    @Autowired
    public PaperTradingReportService(PaperTradingReportPort reportPort, ReplayBacktestSourcePort sourcePort,
                                     StockPort stockPort, TradingSignalQueryPort signalPort,
                                     EarlyMarketPerformancePort performancePort, EarlyMarketFollowUpResultPort followUpPort,
                                     PaperTradingReportProperties properties, OperationalMetricsPort metrics) {
        this(reportPort, sourcePort, stockPort, signalPort, performancePort, followUpPort,
                properties, metrics, Clock.systemUTC());
    }

    PaperTradingReportService(PaperTradingReportPort reportPort, ReplayBacktestSourcePort sourcePort,
                              StockPort stockPort, TradingSignalQueryPort signalPort,
                              EarlyMarketPerformancePort performancePort, EarlyMarketFollowUpResultPort followUpPort,
                              PaperTradingReportProperties properties, OperationalMetricsPort metrics, Clock clock) {
        this.reportPort=reportPort; this.sourcePort=sourcePort; this.stockPort=stockPort; this.signalPort=signalPort;
        this.performancePort=performancePort; this.followUpPort=followUpPort; this.properties=properties;
        this.metrics=metrics; this.clock=clock;
    }

    @Override
    public PaperTradingReportView generateDailyReport(LocalDate tradeDate) {
        Objects.requireNonNull(tradeDate, "tradeDate");
        if (!properties.isEnabled()) throw new IllegalStateException("Paper trading report is disabled");
        PaperTradingReportRun run = reportPort.saveRun(new PaperTradingReportRun(null, tradeDate,
                PaperTradingReportStatus.CREATED, 0, null, 0, 0, 0, null, clock.instant(), null));
        run = reportPort.saveRun(withStatus(run, PaperTradingReportStatus.RUNNING, null));
        try {
            List<PaperTradingReportResult> values = new ArrayList<>();
            values.addAll(earlyResults(run.id(), tradeDate));
            values.addAll(closingResults(run.id(), tradeDate));
            values.addAll(morningNoteResults(run.id(), tradeDate));
            values = reportPort.saveResults(values);
            PaperTradingReportRun completed = reportPort.saveRun(completed(run, values));
            metrics.recordPaperTradingReport(values.stream().anyMatch(v -> v.resultStatus() == PaperTradingResultStatus.DATA_INSUFFICIENT)
                    ? "insufficient" : "success");
            return view(completed, values);
        } catch (RuntimeException exception) {
            reportPort.saveRun(withStatus(run, PaperTradingReportStatus.FAILED, failureReason(exception)));
            metrics.recordPaperTradingReport("failure");
            throw exception;
        }
    }

    @Override public PaperTradingReportView getRun(long runId) {
        PaperTradingReportRun run = reportPort.findRun(runId)
                .orElseThrow(() -> new PaperTradingReportNotFoundException("Paper trading report run not found: " + runId));
        return view(run, reportPort.findResults(runId));
    }
    @Override public List<PaperTradingReportResult> getResults(long runId) {
        if (reportPort.findRun(runId).isEmpty()) throw new PaperTradingReportNotFoundException("Paper trading report run not found: " + runId);
        return reportPort.findResults(runId);
    }
    @Override public PaperTradingReportView getLatestByTradeDate(LocalDate tradeDate) {
        Objects.requireNonNull(tradeDate, "tradeDate");
        PaperTradingReportRun run = reportPort.findLatestRun(tradeDate)
                .orElseThrow(() -> new PaperTradingReportNotFoundException("Paper trading report not found: " + tradeDate));
        return view(run, reportPort.findResults(run.id()));
    }

    private List<PaperTradingReportResult> earlyResults(long runId, LocalDate date) {
        List<TradingSignalRecord> signals = signals(date, EARLY_STRATEGY, SignalType.EARLY_MARKET_ENTRY_CANDIDATE);
        Map<Long, EarlyMarketCandidatePerformance> captures = performancePort.findByTradeDate(date).stream()
                .collect(Collectors.toMap(EarlyMarketCandidatePerformance::signalId, Function.identity(), (a,b)->a));
        Map<Long, EarlyMarketFollowUpRecord> followUps = followUpPort.findByTradeDate(date).stream()
                .collect(Collectors.toMap(EarlyMarketFollowUpRecord::signalId, Function.identity(), (a,b)->a));
        return ranked(signals).stream().map(item -> {
            TradingSignalRecord signal = item.signal();
            List<EarlyMarketIntradayBarSnapshot> bars = sourcePort.findIntradayBars(signal.stockCode(), date);
            LocalTime entryTime = properties.getEarlyMarketEntryTime(); LocalTime exitTime = properties.getEarlyMarketExitTime();
            BigDecimal entry = closeAtOrBefore(bars, entryTime); BigDecimal exit = closeAtOrBefore(bars, exitTime);
            List<EarlyMarketIntradayBarSnapshot> window = bars.stream()
                    .filter(bar -> !bar.barTime().isBefore(entryTime) && !bar.barTime().isAfter(exitTime)).toList();
            BigDecimal high = window.stream().map(EarlyMarketIntradayBarSnapshot::highPrice).max(BigDecimal::compareTo).orElse(null);
            BigDecimal low = window.stream().map(EarlyMarketIntradayBarSnapshot::lowPrice).min(BigDecimal::compareTo).orElse(null);
            List<String> warnings = new ArrayList<>(signal.riskReasons());
            EarlyMarketCandidatePerformance capture = signal.id() == null ? null : captures.get(signal.id());
            if ((entry == null || exit == null || high == null || low == null) && capture != null) {
                if (entry == null) entry = capture.entryReferencePrice(); if (exit == null) exit = capture.priceAt0930();
                if (high == null) high = capture.highUntil0930(); if (low == null) low = capture.lowUntil0930();
                warnings.add("EARLY_MARKET_PERFORMANCE_CAPTURE_FALLBACK");
            }
            EarlyMarketFollowUpRecord followUp = signal.id() == null ? null : followUps.get(signal.id());
            if (high == null && followUp != null) high = followUp.highSince0905();
            return result(runId, date, PaperTradingStrategy.EARLY_MARKET, item.rank(), signal.id(), signal.stockCode(),
                    signal.score(), signal.reasons(), warnings, entry, exit, high, low);
        }).toList();
    }

    private List<PaperTradingReportResult> closingResults(long runId, LocalDate date) {
        return ranked(signals(date, CLOSING_STRATEGY, SignalType.BUY_CANDIDATE)).stream().map(item -> {
            TradingSignalRecord signal = item.signal();
            DailyPrice entryDay = sourcePort.findDailyPrice(signal.stockCode(), date).orElse(null);
            DailyPrice next = sourcePort.findNthDailyPriceAfter(signal.stockCode(), date, 1).orElse(null);
            BigDecimal exit = next == null ? null : properties.getClosingBetExitPolicy() == PaperTradingReportProperties.ClosingBetExitPolicy.NEXT_OPEN
                    ? next.openPrice() : next.closePrice();
            return result(runId, date, PaperTradingStrategy.CLOSING_BET, item.rank(), signal.id(), signal.stockCode(),
                    signal.score(), signal.reasons(), signal.riskReasons(), entryDay == null ? null : entryDay.closePrice(),
                    exit, next == null ? null : next.highPrice(), next == null ? null : next.lowPrice());
        }).toList();
    }

    private List<PaperTradingReportResult> morningNoteResults(long runId, LocalDate date) {
        List<Stock> stocks = stockPort.findAll().stream().filter(Stock::active)
                .sorted(Comparator.comparing(Stock::stockCode)).toList();
        List<PaperTradingReportResult> results = new ArrayList<>();
        for (int i=0; i<stocks.size(); i++) {
            Stock stock = stocks.get(i); DailyPrice price = sourcePort.findDailyPrice(stock.stockCode(), date).orElse(null);
            results.add(result(runId, date, PaperTradingStrategy.MORNING_NOTE, i+1, null, stock.stockCode(), 0,
                    List.of("MORNING_NOTE_ACTIVE_WATCHLIST"), List.of(), price == null ? null : price.openPrice(),
                    price == null ? null : price.closePrice(), price == null ? null : price.highPrice(), price == null ? null : price.lowPrice()));
        }
        return results;
    }

    private PaperTradingReportResult result(long runId, LocalDate date, PaperTradingStrategy strategy, int rank,
                                            Long signalId, String stockCode, int score, List<String> reasons,
                                            List<String> initialWarnings, BigDecimal entry, BigDecimal exit,
                                            BigDecimal high, BigDecimal low) {
        List<String> warnings = new ArrayList<>(initialWarnings);
        if (entry == null || entry.signum() <= 0) warnings.add(MISSING_ENTRY);
        if (exit == null) warnings.add(MISSING_EXIT);
        boolean insufficient = warnings.contains(MISSING_ENTRY) || warnings.contains(MISSING_EXIT);
        BigDecimal returnRate = insufficient ? null : rate(entry, exit);
        BigDecimal mfe = entry == null || entry.signum() <= 0 || high == null ? null : rate(entry, high);
        BigDecimal mae = entry == null || entry.signum() <= 0 || low == null ? null : rate(entry, low);
        PaperTradingResultStatus status = returnRate == null ? PaperTradingResultStatus.DATA_INSUFFICIENT
                : returnRate.signum()>0 ? PaperTradingResultStatus.WIN
                : returnRate.signum()<0 ? PaperTradingResultStatus.LOSS : PaperTradingResultStatus.FLAT;
        return new PaperTradingReportResult(null, runId, date, strategy, stockCode,
                sourcePort.findStockName(stockCode).orElse(stockCode), rank, signalId, score,
                List.copyOf(reasons), List.copyOf(warnings), entry, exit, high, low, mfe, mae,
                returnRate, status, clock.instant());
    }

    static BigDecimal rate(BigDecimal entry, BigDecimal value) {
        return value.subtract(entry).multiply(BigDecimal.valueOf(100)).divide(entry, 6, RoundingMode.HALF_UP);
    }
    private List<TradingSignalRecord> signals(LocalDate date, String strategy, SignalType type) {
        return signalPort.find(new TradingSignalSearchCriteria(null, date, strategy, type, null, null));
    }
    private static List<Ranked> ranked(List<TradingSignalRecord> signals) {
        List<TradingSignalRecord> sorted = signals.stream().sorted(Comparator.comparingInt(TradingSignalRecord::score).reversed()
                .thenComparing(TradingSignalRecord::stockCode)).toList();
        List<Ranked> result = new ArrayList<>(); for(int i=0;i<sorted.size();i++) result.add(new Ranked(i+1, sorted.get(i))); return result;
    }
    private static BigDecimal closeAtOrBefore(List<EarlyMarketIntradayBarSnapshot> bars, LocalTime time) {
        return bars.stream().filter(bar -> !bar.barTime().isAfter(time)).max(Comparator.comparing(EarlyMarketIntradayBarSnapshot::barTime))
                .map(EarlyMarketIntradayBarSnapshot::closePrice).orElse(null);
    }
    private PaperTradingReportRun completed(PaperTradingReportRun run, List<PaperTradingReportResult> results) {
        List<BigDecimal> returns = returns(results);
        return new PaperTradingReportRun(run.id(), run.tradeDate(), PaperTradingReportStatus.COMPLETED, results.size(), average(returns),
                count(results, PaperTradingResultStatus.WIN), count(results, PaperTradingResultStatus.LOSS),
                count(results, PaperTradingResultStatus.FLAT), null, run.createdAt(), clock.instant());
    }
    private PaperTradingReportRun withStatus(PaperTradingReportRun run, PaperTradingReportStatus status, String failure) {
        return new PaperTradingReportRun(run.id(), run.tradeDate(), status, run.totalCandidates(), run.averageReturnRate(),
                run.winCount(), run.lossCount(), run.flatCount(), failure, run.createdAt(),
                status == PaperTradingReportStatus.FAILED ? clock.instant() : null);
    }
    static PaperTradingReportView view(PaperTradingReportRun run, List<PaperTradingReportResult> results) {
        List<BigDecimal> returns=returns(results); BigDecimal winRate=returns.isEmpty()?null:BigDecimal.valueOf(run.winCount()*100L)
                .divide(BigDecimal.valueOf(returns.size()),6,RoundingMode.HALF_UP);
        return new PaperTradingReportView(run, winRate, count(results, PaperTradingResultStatus.DATA_INSUFFICIENT),
                breakdown(results, r->List.of(r.strategy().name())), breakdown(results, PaperTradingReportResult::reasons),
                breakdown(results, PaperTradingReportResult::warnings), top(results, true), top(results, false));
    }
    private static List<PaperTradingPerformanceBreakdown> breakdown(List<PaperTradingReportResult> results,
                                                                     Function<PaperTradingReportResult,List<String>> keys) {
        Map<String,List<PaperTradingReportResult>> groups=new TreeMap<>();
        results.forEach(r->keys.apply(r).forEach(k->groups.computeIfAbsent(k, ignored->new ArrayList<>()).add(r)));
        return groups.entrySet().stream().map(e->{ List<BigDecimal> values=returns(e.getValue());
            int wins=count(e.getValue(),PaperTradingResultStatus.WIN); BigDecimal wr=values.isEmpty()?null:BigDecimal.valueOf(wins*100L)
                    .divide(BigDecimal.valueOf(values.size()),6,RoundingMode.HALF_UP);
            return new PaperTradingPerformanceBreakdown(e.getKey(),e.getValue().size(),values.size(),wr,average(values)); }).toList();
    }
    private static List<PaperTradingReportResult> top(List<PaperTradingReportResult> values, boolean winners) {
        Comparator<PaperTradingReportResult> comparator=Comparator.comparing(PaperTradingReportResult::returnRate);
        if(winners) comparator=comparator.reversed();
        PaperTradingResultStatus status = winners ? PaperTradingResultStatus.WIN : PaperTradingResultStatus.LOSS;
        return values.stream().filter(v->v.resultStatus()==status).sorted(comparator).limit(5).toList();
    }
    private static List<BigDecimal> returns(List<PaperTradingReportResult> values) { return values.stream().map(PaperTradingReportResult::returnRate).filter(Objects::nonNull).toList(); }
    private static BigDecimal average(List<BigDecimal> values) { return values.isEmpty()?null:values.stream().reduce(BigDecimal.ZERO,BigDecimal::add).divide(BigDecimal.valueOf(values.size()),6,RoundingMode.HALF_UP); }
    private static int count(List<PaperTradingReportResult> values, PaperTradingResultStatus status) { return (int)values.stream().filter(v->v.resultStatus()==status).count(); }
    private static String failureReason(RuntimeException exception) { String value=exception.getClass().getSimpleName()+": "+exception.getMessage(); return value.length()<=1000?value:value.substring(0,1000); }
    private record Ranked(int rank, TradingSignalRecord signal) { }
}
