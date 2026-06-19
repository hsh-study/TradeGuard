package seokhoon.trade.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.TradingSignalSearchCriteria;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.config.PaperTradingReportProperties;
import seokhoon.trade.domain.market.*;
import seokhoon.trade.domain.research.*;
import seokhoon.trade.domain.strategy.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaperTradingReportServiceTest {
    private static final LocalDate DATE=LocalDate.of(2026,6,15);
    private ReplayBacktestSourcePort source; private StockPort stocks; private TradingSignalQueryPort signals;
    private EarlyMarketPerformancePort performances; private EarlyMarketFollowUpResultPort followUps;
    private InMemoryPort reports; private PaperTradingReportProperties properties; private PaperTradingReportService service;

    @BeforeEach void setUp() {
        source=mock(ReplayBacktestSourcePort.class); stocks=mock(StockPort.class); signals=mock(TradingSignalQueryPort.class);
        performances=mock(EarlyMarketPerformancePort.class); followUps=mock(EarlyMarketFollowUpResultPort.class);
        reports=new InMemoryPort(); properties=new PaperTradingReportProperties();
        when(stocks.findAll()).thenReturn(List.of()); when(performances.findByTradeDate(any())).thenReturn(List.of());
        when(followUps.findByTradeDate(any())).thenReturn(List.of()); when(source.findStockName(any())).thenReturn(Optional.of("삼성전자"));
        service=new PaperTradingReportService(reports,source,stocks,signals,performances,followUps,properties,
                OperationalMetricsPort.noop(),Clock.fixed(Instant.parse("2026-06-15T07:10:00Z"),ZoneOffset.UTC));
    }

    @Test void createsDailyReportAndCalculatesEarlyMarketEntryExitMfeMaeAndReasonBreakdown() {
        when(signals.find(any())).thenAnswer(invocation -> {
            TradingSignalSearchCriteria criteria=invocation.getArgument(0);
            return criteria.signalType()==SignalType.EARLY_MARKET_ENTRY_CANDIDATE ? List.of(signal(1L,criteria.signalType(),80,List.of("VWAP_ABOVE"))) : List.of();
        });
        when(source.findIntradayBars("005930",DATE)).thenReturn(List.of(
                bar(LocalTime.of(9,5),"100","101","99"), bar(LocalTime.of(9,20),"103","105","98"),
                bar(LocalTime.of(9,31),"102","104","101")));

        var view=service.generateDailyReport(DATE);

        PaperTradingReportResult result=reports.results.getFirst();
        assertThat(view.run().status()).isEqualTo(PaperTradingReportStatus.COMPLETED);
        assertThat(result.referenceEntryPrice()).isEqualByComparingTo("100");
        assertThat(result.referenceExitPrice()).isEqualByComparingTo("102");
        assertThat(result.maxFavorableExcursion()).isEqualByComparingTo("5.000000");
        assertThat(result.maxAdverseExcursion()).isEqualByComparingTo("-2.000000");
        assertThat(view.performanceByReason()).singleElement().satisfies(v -> {
            assertThat(v.key()).isEqualTo("VWAP_ABOVE"); assertThat(v.averageReturnRate()).isEqualByComparingTo("2.000000");
        });
    }

    @Test void usesNextTradingDayCloseForClosingBet() {
        when(signals.find(any())).thenAnswer(invocation -> {
            TradingSignalSearchCriteria criteria=invocation.getArgument(0);
            return criteria.signalType()==SignalType.BUY_CANDIDATE ? List.of(signal(2L,criteria.signalType(),75,List.of("CLOSE_NEAR_HIGH"))) : List.of();
        });
        when(source.findDailyPrice("005930",DATE)).thenReturn(Optional.of(daily(DATE,"100","100","100","100")));
        when(source.findNthDailyPriceAfter("005930",DATE,1)).thenReturn(Optional.of(daily(DATE.plusDays(1),"101","112","99","110")));

        service.generateDailyReport(DATE);

        PaperTradingReportResult result=reports.results.getFirst();
        assertThat(result.strategy()).isEqualTo(PaperTradingStrategy.CLOSING_BET);
        assertThat(result.referenceExitPrice()).isEqualByComparingTo("110");
        assertThat(result.returnRate()).isEqualByComparingTo("10.000000");
    }

    @Test void storesDataInsufficientWhenPricesAreMissing() {
        when(signals.find(any())).thenAnswer(invocation -> {
            TradingSignalSearchCriteria criteria=invocation.getArgument(0);
            return criteria.signalType()==SignalType.BUY_CANDIDATE ? List.of(signal(2L,criteria.signalType(),75,List.of())) : List.of();
        });
        var view=service.generateDailyReport(DATE);
        assertThat(reports.results.getFirst().resultStatus()).isEqualTo(PaperTradingResultStatus.DATA_INSUFFICIENT);
        assertThat(view.dataInsufficientCount()).isEqualTo(1);
    }

    @Test void hasNoProviderBrokerOrOrderDependency() {
        Set<String> names=new HashSet<>();
        Arrays.stream(PaperTradingReportService.class.getDeclaredConstructors()).flatMap(c->Arrays.stream(c.getParameterTypes()))
                .map(Class::getSimpleName).forEach(names::add);
        assertThat(names).noneMatch(name->name.contains("Provider")||name.contains("Broker")||name.contains("Order"));
    }

    private static TradingSignalRecord signal(long id,SignalType type,int score,List<String> reasons) {
        return new TradingSignalRecord(id,type==SignalType.BUY_CANDIDATE?"CLOSING_BET":"EARLY_MARKET_BREAKOUT","005930",DATE,
                type,score,reasons,List.of(),TradingSignalStatus.CREATED);
    }
    private static EarlyMarketIntradayBarSnapshot bar(LocalTime time,String close,String high,String low) {
        return new EarlyMarketIntradayBarSnapshot(null,DATE,"005930",Instant.EPOCH,time,BarInterval.ONE_MINUTE,
                new BigDecimal(close),new BigDecimal(high),new BigDecimal(low),new BigDecimal(close),1,BigDecimal.ONE,new BigDecimal(close),"STORED");
    }
    private static DailyPrice daily(LocalDate date,String open,String high,String low,String close) {
        return new DailyPrice("005930",date,new BigDecimal(open),new BigDecimal(high),new BigDecimal(low),new BigDecimal(close),1,BigDecimal.ONE);
    }
    private static class InMemoryPort implements PaperTradingReportPort {
        PaperTradingReportRun run; List<PaperTradingReportResult> results=List.of();
        public PaperTradingReportRun saveRun(PaperTradingReportRun value) { run=value.id()==null?new PaperTradingReportRun(1L,value.tradeDate(),value.status(),value.totalCandidates(),value.averageReturnRate(),value.winCount(),value.lossCount(),value.flatCount(),value.failureReason(),value.createdAt(),value.completedAt()):value; return run; }
        public List<PaperTradingReportResult> saveResults(List<PaperTradingReportResult> values) { results=values; return values; }
        public Optional<PaperTradingReportRun> findRun(long id) { return Optional.ofNullable(run); }
        public Optional<PaperTradingReportRun> findLatestRun(LocalDate date) { return Optional.ofNullable(run); }
        public List<PaperTradingReportResult> findResults(long id) { return results; }
    }
}
