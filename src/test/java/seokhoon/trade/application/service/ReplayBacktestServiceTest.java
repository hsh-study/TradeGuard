package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.TradingSignalSearchCriteria;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.domain.market.*;
import seokhoon.trade.domain.research.*;
import seokhoon.trade.domain.strategy.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class ReplayBacktestServiceTest {
    private static final LocalDate DATE = LocalDate.of(2026, 6, 1);
    private static final Instant NOW = Instant.parse("2026-06-18T00:00:00Z");

    @Test
    void createsClosingBetRunAndUsesNthTradingDayExit() {
        FakeSource source = new FakeSource();
        source.daily.put(key("005930", DATE), price(DATE, "100"));
        source.after.put("005930", List.of(price(DATE.plusDays(1), "101"), price(DATE.plusDays(3), "110")));
        Fixture fixture = fixture(source, List.of(signal(SignalType.BUY_CANDIDATE, 80, List.of("MA5_ABOVE_MA20"))));

        var view = fixture.service.runClosingBet(DATE, DATE, 2);

        assertThat(view.run().status()).isEqualTo(ReplayBacktestStatus.COMPLETED);
        assertThat(view.run().totalCandidates()).isEqualTo(1);
        assertThat(view.run().winCount()).isEqualTo(1);
        assertThat(fixture.port.results.getFirst().exitReferencePrice()).isEqualByComparingTo("110");
        assertThat(fixture.port.results.getFirst().returnRate()).isEqualByComparingTo("10.000000");
        assertThat(view.performanceByReason()).singleElement().satisfies(item -> {
            assertThat(item.key()).isEqualTo("MA5_ABOVE_MA20");
            assertThat(item.averageReturnRate()).isEqualByComparingTo("10.000000");
        });
    }

    @Test
    void createsEarlyMarketRunFromStoredIntradayBars() {
        FakeSource source = new FakeSource();
        source.bars = List.of(bar(LocalTime.of(9, 5), "100"), bar(LocalTime.of(9, 31), "102"));
        Fixture fixture = fixture(source, List.of(signal(SignalType.EARLY_MARKET_ENTRY_CANDIDATE, 75,
                List.of("EARLY_MARKET_OPENING_09_05"))));

        var view = fixture.service.runEarlyMarket(DATE, DATE, LocalTime.of(9, 5), LocalTime.of(9, 31));

        assertThat(view.run().strategy()).isEqualTo(ReplayBacktestStrategy.EARLY_MARKET);
        assertThat(fixture.port.results.getFirst().returnRate()).isEqualByComparingTo("2.000000");
        assertThat(fixture.metrics).containsExactly("EARLY_MARKET:success");
    }

    @Test
    void persistsDataInsufficientWhenReferencePriceIsMissing() {
        Fixture fixture = fixture(new FakeSource(), List.of(signal(SignalType.BUY_CANDIDATE, 70, List.of("REASON"))));

        var view = fixture.service.runClosingBet(DATE, DATE, 1);

        ReplayBacktestResult result = fixture.port.results.getFirst();
        assertThat(result.resultStatus()).isEqualTo(ReplayBacktestResultStatus.DATA_INSUFFICIENT);
        assertThat(result.warnings()).containsExactly("MISSING_ENTRY_REFERENCE_PRICE", "MISSING_EXIT_REFERENCE_PRICE");
        assertThat(view.run().averageReturnRate()).isNull();
        assertThat(fixture.metrics).containsExactly("CLOSING_BET:insufficient");
    }

    @Test
    void calculatesReturnRateWithSixDecimalPrecision() {
        assertThat(ReplayBacktestService.calculateReturnRate(new BigDecimal("300"), new BigDecimal("310")))
                .isEqualByComparingTo("3.333333");
    }

    @Test
    void hasNoProviderOrOrderExecutionDependency() {
        Set<Class<?>> dependencyTypes = Arrays.stream(ReplayBacktestService.class.getDeclaredConstructors())
                .flatMap(constructor -> Arrays.stream(constructor.getParameterTypes())).collect(java.util.stream.Collectors.toSet());

        assertThat(dependencyTypes).contains(ReplayBacktestPort.class, ReplayBacktestSourcePort.class,
                TradingSignalQueryPort.class, OperationalMetricsPort.class);
        assertThat(dependencyTypes.stream().map(Class::getSimpleName))
                .noneMatch(name -> name.contains("Provider") || name.contains("Broker") || name.contains("Order"));
    }

    private static Fixture fixture(FakeSource source, List<TradingSignalRecord> signals) {
        FakeReplayPort port = new FakeReplayPort();
        List<String> metrics = new ArrayList<>();
        OperationalMetricsPort metricsPort = new NoopMetrics() {
            @Override public void recordReplayBacktest(String strategy, String result) { metrics.add(strategy + ":" + result); }
        };
        TradingSignalQueryPort query = criteria -> signals.stream()
                .filter(s -> s.signalDate().equals(criteria.signalDate()))
                .filter(s -> s.signalType() == criteria.signalType()).toList();
        return new Fixture(new ReplayBacktestService(port, source, query, metricsPort,
                Clock.fixed(NOW, ZoneOffset.UTC)), port, metrics);
    }

    private static TradingSignalRecord signal(SignalType type, int score, List<String> reasons) {
        return new TradingSignalRecord(1L, type == SignalType.BUY_CANDIDATE ? "CLOSING_BET" : "EARLY_MARKET_BREAKOUT",
                "005930", DATE, type, score, reasons, List.of(), TradingSignalStatus.CREATED);
    }
    private static DailyPrice price(LocalDate date, String close) {
        BigDecimal value = new BigDecimal(close);
        return new DailyPrice("005930", date, value, value, value, value, 1, value);
    }
    private static EarlyMarketIntradayBarSnapshot bar(LocalTime time, String close) {
        BigDecimal value = new BigDecimal(close);
        return new EarlyMarketIntradayBarSnapshot(null, DATE, "005930", NOW, time, BarInterval.ONE_MINUTE,
                value, value, value, value, 1, value, value, "STORED");
    }
    private static String key(String code, LocalDate date) { return code + ":" + date; }

    private record Fixture(ReplayBacktestService service, FakeReplayPort port, List<String> metrics) { }

    private static class FakeSource implements ReplayBacktestSourcePort {
        final Map<String, DailyPrice> daily = new HashMap<>();
        final Map<String, List<DailyPrice>> after = new HashMap<>();
        List<EarlyMarketIntradayBarSnapshot> bars = List.of();
        public Optional<String> findStockName(String stockCode) { return Optional.of("삼성전자"); }
        public Optional<DailyPrice> findDailyPrice(String stockCode, LocalDate tradeDate) { return Optional.ofNullable(daily.get(key(stockCode, tradeDate))); }
        public Optional<DailyPrice> findNthDailyPriceAfter(String stockCode, LocalDate tradeDate, int days) {
            List<DailyPrice> values = after.getOrDefault(stockCode, List.of());
            return values.size() < days ? Optional.empty() : Optional.of(values.get(days - 1));
        }
        public List<EarlyMarketIntradayBarSnapshot> findIntradayBars(String stockCode, LocalDate tradeDate) { return bars; }
    }

    private static class FakeReplayPort implements ReplayBacktestPort {
        ReplayBacktestRun run; List<ReplayBacktestResult> results = List.of();
        public ReplayBacktestRun saveRun(ReplayBacktestRun value) {
            run = value.id() == null ? new ReplayBacktestRun(1L, value.strategy(), value.fromDate(), value.toDate(), value.status(),
                    value.parameterSnapshot(), value.totalCandidates(), value.winCount(), value.lossCount(), value.averageReturnRate(),
                    value.maxReturnRate(), value.minReturnRate(), value.failureReason(), value.createdAt(), value.completedAt()) : value;
            return run;
        }
        public List<ReplayBacktestResult> saveResults(List<ReplayBacktestResult> values) { results = values; return values; }
        public Optional<ReplayBacktestRun> findRun(long id) { return Optional.ofNullable(run); }
        public List<ReplayBacktestResult> findResults(long id) { return results; }
    }

    private abstract static class NoopMetrics implements OperationalMetricsPort {
        public void recordSchedulerExecution(seokhoon.trade.domain.scheduler.SchedulerName n, seokhoon.trade.domain.scheduler.SchedulerExecutionStatus s) {}
        public void recordSchedulerSelected(seokhoon.trade.domain.scheduler.SchedulerName n,int c) {} public void recordSchedulerNotification(seokhoon.trade.domain.scheduler.SchedulerName n,boolean s) {}
        public void recordOrderRequest(String s) {} public void recordBrokerFailure(boolean b) {} public void recordOrderRetry(String s) {} public void recordOrderRetryRecovery(String s) {}
        public void recordDiscordNotification(String s) {} public void recordKisReadOnly(String o,String r) {} public void recordKisTokenIssue(String e,String r) {} public void recordKisTokenCache(String e,String r) {} public void recordKisTokenStore(String c,String r) {}
        public void recordAfterHoursLookup(String r) {} public void recordIntradayBarLookup(String r) {} public void recordEarlyMarketPerformanceCapture(String r) {} public void recordEarlyMarketFollowUp(String d) {}
        public void recordEarlyMarketPriceAction(String r) {} public void recordEarlyMarketReport(String r) {} public void recordEarlyMarketPeriodReport(String r) {} public void recordEarlyMarketExperiment(String r) {}
        public void recordEarlyMarketExperimentCompare(String r) {} public void recordEarlyMarketBacktest(String r) {} public void recordEarlyMarketFollowUpPersist(String r) {} public void recordMarketCalendarSync(String r,int y) {}
        public void recordMarketCalendarLookup(String r,String m) {} public void recordMarketCalendarOverride(String r) {} public void recordMarketCalendarValidation(String r) {} public void recordEarlyMarketDataCapture(String c,String r) {}
        public void recordLiveOrderRequest(String s,String st) {} public void recordLiveOrderSubmit(String s,String r) {} public void recordLivePositionExitEvaluation(String r) {} public void recordLiveOrderReconciliation(String r) {}
        public void recordLiveOrderCancel(String r) {} public void recordLiveTradingReadiness(String r) {} public void recordIndicatorWarmUp(String r) {} public void recordIndicatorDataSufficiency(String r) {}
        public void recordResearchThesis(String s) {} public void recordResearchCatalyst(String s) {} public void recordResearchMorningNote(String r) {} public void recordResearchSectorSnapshot(String r) {}
        public void recordResearchEarningsAnalysis(String r) {} public void recordResearchFinancialImport(String r) {} public void recordResearchValuationImport(String r) {} public void recordResearchValuationAutoSnapshot(String r) {}
        public void recordResearchSharesOutstanding(String r) {} public void recordResearchEarningsEvent(String s) {} public void recordResearchEarningsPreview(String r) {} public void recordResearchPostEarningsReview(String t) {}
        public void recordDartFinancialImport(String r) {} public void recordDartProvider(String o,String r) {} public void recordDartCorpCodeImport(String r) {} public void recordSharesOutstandingImport(String r) {}
        public void recordCatalystEvidence(String t,String c) {} public void recordDisclosureEvidenceImport(String p,String r) {} public void recordMarketIndexImport(String p,String r) {} public void recordSectorImport(String r) {}
        public void recordInvestorFlowImport(String s,String r) {} public void recordInvestorFlowDiagnostic(String s,String r) {} public void recordInvestorFlowReadiness(String r) {} public void recordSupplyDemandAnalysis(String r) {}
        public void recordSupplyDemandStrategyAdjustment(String s,String r) {}
    }
}
