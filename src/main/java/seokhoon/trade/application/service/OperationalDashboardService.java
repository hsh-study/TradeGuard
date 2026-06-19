package seokhoon.trade.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.in.*;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.config.DartProperties;
import seokhoon.trade.config.DisclosureActualProviderProperties;
import seokhoon.trade.domain.kis.KisEnvironment;
import seokhoon.trade.domain.market.*;
import seokhoon.trade.domain.order.LiveTradingReadinessReport;
import seokhoon.trade.domain.research.*;
import seokhoon.trade.domain.scheduler.*;
import seokhoon.trade.domain.stock.Stock;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static seokhoon.trade.application.port.in.OperationalDashboardSummary.*;

@Service
@Transactional(readOnly = true)
public class OperationalDashboardService implements GetOperationalDashboardUseCase {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final Set<SchedulerName> CORE_SCHEDULERS = EnumSet.of(
            SchedulerName.INVESTOR_FLOW_IMPORT, SchedulerName.SUPPLY_DEMAND_ANALYSIS,
            SchedulerName.EARLY_MARKET_PRE_OPEN_830, SchedulerName.EARLY_MARKET_OPENING_905,
            SchedulerName.CLOSING_BET_PRE_SCAN_14, SchedulerName.CLOSING_BET_FINAL_REVIEW_15);

    private final LoadMarketCalendarUseCase calendar;
    private final MorningNotePort morningNotes;
    private final EarlyMarketDataCapturePort captures;
    private final EarlyMarketFollowUpResultPort followUps;
    private final EarlyMarketPerformancePort performances;
    private final SchedulerExecutionHistoryPort schedulerHistories;
    private final GetInvestorFlowReadinessUseCase investorReadiness;
    private final SupplyDemandSnapshotPort supplyDemand;
    private final EarningsAnalysisPort earningsAnalyses;
    private final EarningsEventPort earningsEvents;
    private final DartFinancialImportHistoryPort dartFinancials;
    private final DartCorpCodeImportHistoryPort dartCorpCodes;
    private final DartCorpMappingPort dartMappings;
    private final DartProperties dartProperties;
    private final DisclosureActualProviderProperties disclosureProperties;
    private final DisclosureEvidenceImportHistoryPort disclosureHistories;
    private final CatalystEvidencePort catalystEvidences;
    private final StockPort stocks;
    private final StockSectorMappingPort stockSectors;
    private final ValuationSnapshotPort valuations;
    private final SharesOutstandingSnapshotPort shares;
    private final PaperTradingReportPort paperReports;
    private final ReplayBacktestPort replayBacktests;
    private final KisTokenUseCases.ManageKisTokenUseCase tokens;
    private final LiveTradingReadinessUseCase liveReadiness;
    private final boolean discordEnabled;
    private final Clock clock;

    @Autowired
    public OperationalDashboardService(LoadMarketCalendarUseCase calendar, MorningNotePort morningNotes,
            EarlyMarketDataCapturePort captures, EarlyMarketFollowUpResultPort followUps,
            EarlyMarketPerformancePort performances, SchedulerExecutionHistoryPort schedulerHistories,
            GetInvestorFlowReadinessUseCase investorReadiness, SupplyDemandSnapshotPort supplyDemand,
            EarningsAnalysisPort earningsAnalyses, EarningsEventPort earningsEvents,
            DartFinancialImportHistoryPort dartFinancials, DartCorpCodeImportHistoryPort dartCorpCodes,
            DartCorpMappingPort dartMappings, DartProperties dartProperties, StockPort stocks,
            DisclosureActualProviderProperties disclosureProperties,
            DisclosureEvidenceImportHistoryPort disclosureHistories, CatalystEvidencePort catalystEvidences,
            StockSectorMappingPort stockSectors,
            ValuationSnapshotPort valuations, SharesOutstandingSnapshotPort shares,
            PaperTradingReportPort paperReports, ReplayBacktestPort replayBacktests,
            KisTokenUseCases.ManageKisTokenUseCase tokens, LiveTradingReadinessUseCase liveReadiness,
            @Value("${tradeguard.notification.discord.webhook-url:}") String discordWebhookUrl) {
        this(calendar, morningNotes, captures, followUps, performances, schedulerHistories,
                investorReadiness, supplyDemand, earningsAnalyses, earningsEvents, dartFinancials,
                dartCorpCodes, dartMappings, dartProperties, stocks, disclosureProperties,
                disclosureHistories, catalystEvidences, stockSectors, valuations, shares, paperReports,
                replayBacktests, tokens, liveReadiness, discordWebhookUrl, Clock.systemUTC());
    }

    OperationalDashboardService(LoadMarketCalendarUseCase calendar, MorningNotePort morningNotes,
            EarlyMarketDataCapturePort captures, EarlyMarketFollowUpResultPort followUps,
            EarlyMarketPerformancePort performances, SchedulerExecutionHistoryPort schedulerHistories,
            GetInvestorFlowReadinessUseCase investorReadiness, SupplyDemandSnapshotPort supplyDemand,
            EarningsAnalysisPort earningsAnalyses, EarningsEventPort earningsEvents,
            DartFinancialImportHistoryPort dartFinancials, DartCorpCodeImportHistoryPort dartCorpCodes,
            DartCorpMappingPort dartMappings, DartProperties dartProperties, StockPort stocks,
            DisclosureActualProviderProperties disclosureProperties,
            DisclosureEvidenceImportHistoryPort disclosureHistories, CatalystEvidencePort catalystEvidences,
            StockSectorMappingPort stockSectors,
            ValuationSnapshotPort valuations, SharesOutstandingSnapshotPort shares,
            PaperTradingReportPort paperReports, ReplayBacktestPort replayBacktests,
            KisTokenUseCases.ManageKisTokenUseCase tokens, LiveTradingReadinessUseCase liveReadiness,
            String discordWebhookUrl, Clock clock) {
        this.calendar=calendar; this.morningNotes=morningNotes; this.captures=captures; this.followUps=followUps;
        this.performances=performances; this.schedulerHistories=schedulerHistories;
        this.investorReadiness=investorReadiness; this.supplyDemand=supplyDemand;
        this.earningsAnalyses=earningsAnalyses; this.earningsEvents=earningsEvents;
        this.dartFinancials=dartFinancials; this.dartCorpCodes=dartCorpCodes; this.dartMappings=dartMappings;
        this.dartProperties=dartProperties; this.stocks=stocks; this.disclosureProperties=disclosureProperties;
        this.disclosureHistories=disclosureHistories; this.catalystEvidences=catalystEvidences;
        this.stockSectors=stockSectors;
        this.valuations=valuations; this.shares=shares;
        this.paperReports=paperReports; this.replayBacktests=replayBacktests; this.tokens=tokens;
        this.liveReadiness=liveReadiness;
        this.discordEnabled=discordWebhookUrl!=null && !discordWebhookUrl.isBlank(); this.clock=clock;
    }

    @Override public OperationalDashboardSummary getTodayDashboard() {
        return getDashboard(LocalDate.now(clock.withZone(SEOUL)));
    }

    @Override public OperationalDashboardSummary getDashboard(LocalDate baseDate) {
        Objects.requireNonNull(baseDate, "baseDate");
        List<SchedulerExecutionHistoryRecord> runs = schedulerHistories.find(baseDate, null, null);
        MarketDateStatus market = market(baseDate);
        MorningNoteStatus note = morningNote(baseDate);
        EarlyMarketStatus early = early(baseDate, runs);
        ClosingBetStatus closing = closing(runs);
        InvestorFlowStatus investor = investor(baseDate);
        EarningsStatus earnings = earnings(baseDate);
        List<Stock> activeStocks = stocks.findAll().stream().filter(Stock::active).toList();
        DartStatus dart = dart(activeStocks);
        ValuationStatus valuation = valuation(baseDate, activeStocks, runs);
        OperationalDashboardSummary.PaperTradingReportStatus paper = paper(baseDate);
        OperationalDashboardSummary.ReplayBacktestStatus replay = replay(baseDate);
        SchedulerStatus scheduler = scheduler(runs);
        KisTokenStatus token = token();
        LiveTradingReadinessStatus live = live();

        List<String> blocking = new ArrayList<>(); List<String> warnings = new ArrayList<>();
        List<String> actions = new ArrayList<>();
        if (!market.warnings().isEmpty()) blocking.add("MARKET_CALENDAR_UNCERTAIN");
        if (!note.generated()) { blocking.add("MORNING_NOTE_NOT_GENERATED"); actions.add("Run Morning Note"); }
        if (investor.providerEnabled() && !investor.amountUnitVerified()) {
            blocking.add("INVESTOR_FLOW_AMOUNT_UNIT_UNVERIFIED"); actions.add("Verify KIS investor flow amount unit");
        }
        boolean coreFailure = runs.stream().anyMatch(r -> CORE_SCHEDULERS.contains(r.schedulerName())
                && r.status() == SchedulerExecutionStatus.FAILED);
        if (coreFailure) { blocking.add("CORE_SCHEDULER_FAILED"); actions.add("Check failed scheduler executions"); }
        if ("INVALID".equals(token.realTokenStatus()) || "INVALID".equals(token.demoTokenStatus())) {
            blocking.add("KIS_TOKEN_INVALID"); actions.add("Refresh KIS token");
        }
        if (live.liveTradingEnabled() && !live.ready()) {
            blocking.add("LIVE_TRADING_NOT_READY"); actions.add("Keep live trading disabled until readiness is green");
        }
        if (live.killSwitchEnabled()) blocking.add("LIVE_TRADING_KILL_SWITCH_ENABLED");
        if (dart.failedImportCount() >= 2) { blocking.add("DART_IMPORT_REPEATED_FAILURE"); actions.add("Run DART financial import"); }
        if (!paper.generated()) { blocking.add("PAPER_TRADING_REPORT_NOT_GENERATED"); actions.add("Generate Paper Trading Report"); }
        if (valuation.insufficientCount() > 0) actions.add("Run valuation snapshot generation");

        addWarnings(warnings, market.warnings(), note.warnings(), early.warnings(), closing.warnings(),
                investor.warnings(), earnings.warnings(), dart.warnings(), valuation.warnings(), paper.warnings(),
                replay.warnings(), scheduler.warnings(), token.warnings(), live.warnings());
        Set<String> sectorMappedStocks = stockSectors.findAllMappings().stream()
                .map(StockSectorMapping::stockCode).collect(Collectors.toSet());
        if (activeStocks.stream().anyMatch(stock -> !sectorMappedStocks.contains(stock.stockCode()))) {
            warnings.add("SECTOR_MAPPING_MISSING");
        }
        return new OperationalDashboardSummary(baseDate, market, note, early, closing, investor, earnings,
                dart, valuation, paper, replay, scheduler, token, live, distinct(blocking), distinct(warnings),
                distinct(actions), clock.instant());
    }

    private MarketDateStatus market(LocalDate date) {
        try {
            MarketCalendarView view = calendar.load(date);
            return new MarketDateStatus(date, view.tradingDay(), view.previousTradingDay(),
                    view.nextTradingDay(), "CONFIGURED_MARKET_CALENDAR", List.of());
        } catch (RuntimeException e) {
            return new MarketDateStatus(date, false, null, null, "UNAVAILABLE", List.of("MARKET_CALENDAR_UNAVAILABLE"));
        }
    }

    private MorningNoteStatus morningNote(LocalDate date) {
        return morningNotes.findByTradeDate(date).map(note -> {
            List<String> items = note.actionItems().lines().map(String::trim).filter(v -> !v.isBlank()).toList();
            int critical = (int) items.stream().filter(v -> v.toUpperCase(Locale.ROOT).matches(".*(CRITICAL|BLOCKING|긴급).*" )).count();
            List<String> warnings = discordEnabled ? List.of() : List.of("DISCORD_DISABLED");
            return new MorningNoteStatus(true, note.tradeDate(), items.size(), critical, note.createdAt(), discordEnabled, warnings);
        }).orElseGet(() -> new MorningNoteStatus(false, date, 0, 0, null, discordEnabled,
                discordEnabled ? List.of() : List.of("DISCORD_DISABLED")));
    }

    private EarlyMarketStatus early(LocalDate date, List<SchedulerExecutionHistoryRecord> runs) {
        List<EarlyMarketDataCapture> values = captures.findCaptures(date);
        String captureStatus = values.isEmpty() ? "NOT_CAPTURED" : values.stream().anyMatch(v -> v.status()==EarlyMarketCaptureStatus.FAILED)
                ? "FAILED" : values.stream().anyMatch(v -> v.status()==EarlyMarketCaptureStatus.PARTIAL) ? "PARTIAL" : "SUCCEEDED";
        List<SchedulerExecutionHistoryRecord> relevant = runs.stream().filter(r -> r.schedulerName().name().startsWith("EARLY_MARKET_")).toList();
        return new EarlyMarketStatus(selected(runs, SchedulerName.EARLY_MARKET_PRE_OPEN_830),
                selected(runs, SchedulerName.EARLY_MARKET_OPENING_905), followUps.findByTradeDate(date).size(),
                performances.findByTradeDate(date).size(), captureStatus, latestStatus(relevant),
                values.isEmpty() ? List.of("EARLY_MARKET_DATA_NOT_CAPTURED") : List.of());
    }

    private ClosingBetStatus closing(List<SchedulerExecutionHistoryRecord> runs) {
        String pre = latestStatus(runs, SchedulerName.CLOSING_BET_PRE_SCAN_14);
        String fin = latestStatus(runs, SchedulerName.CLOSING_BET_FINAL_REVIEW_15);
        return new ClosingBetStatus(selected(runs, SchedulerName.CLOSING_BET_PRE_SCAN_14),
                selected(runs, SchedulerName.CLOSING_BET_FINAL_REVIEW_15), pre, fin,
                "NOT_RUN".equals(pre) || "NOT_RUN".equals(fin) ? List.of("CLOSING_BET_RUN_INCOMPLETE") : List.of());
    }

    private InvestorFlowStatus investor(LocalDate date) {
        InvestorFlowReadiness r = investorReadiness.getReadiness();
        List<StockSupplyDemandSnapshot> snapshots = supplyDemand.findByTradeDate(date);
        return new InvestorFlowStatus(r.ready(), r.providerEnabled(), value(r.amountUnit()), r.amountUnitVerified(),
                value(r.latestStockImportStatus()), value(r.latestMarketImportStatus()),
                value(r.latestSupplyDemandAnalysisStatus()), count(snapshots, SupplyDemandStatus.STRONG_ACCUMULATION),
                count(snapshots, SupplyDemandStatus.DISTRIBUTION), r.warnings());
    }

    private EarningsStatus earnings(LocalDate date) {
        List<EarningsAnalysisSnapshot> values = earningsAnalyses.findByBaseDate(date);
        int insufficient = countEarnings(values, EarningsAnalysisStatus.DATA_INSUFFICIENT);
        int upcoming = earningsEvents.findByStatusAndExpectedAnnouncementDateBetween(EarningsEventStatus.SCHEDULED, date, date.plusDays(30)).size();
        List<String> warnings = insufficient > 0 ? List.of("EARNINGS_DATA_INSUFFICIENT") : List.of();
        return new EarningsStatus(values.size(), countEarnings(values, EarningsAnalysisStatus.STRONG),
                countEarnings(values, EarningsAnalysisStatus.WEAK), insufficient, upcoming,
                earningsEvents.findByStatusAndExpectedAnnouncementDateBetween(EarningsEventStatus.ANNOUNCED, date.minusDays(30), date).size(), warnings);
    }

    private DartStatus dart(List<Stock> active) {
        List<DartFinancialImportHistory> histories = active.stream()
                .flatMap(s -> dartFinancials.findHistoriesByStockCode(s.stockCode()).stream()).toList();
        Comparator<DartFinancialImportHistory> byRequest = Comparator.comparing(DartFinancialImportHistory::requestedAt);
        String latestFinancial = histories.stream().max(byRequest).map(v -> v.status().name()).orElse("NOT_RUN");
        List<DartCorpCodeImportHistory> corpRuns = dartCorpCodes.findAllCorpCodeImports();
        String latestCorp = corpRuns.stream().max(Comparator.comparing(DartCorpCodeImportHistory::requestedAt))
                .map(v -> v.status().name()).orElse("NOT_RUN");
        Set<String> mapped = dartMappings.findAll().stream().map(v -> v.stockCode()).collect(Collectors.toSet());
        int missing = (int) active.stream().filter(s -> !mapped.contains(s.stockCode())).count();
        int failed = (int) histories.stream().filter(v -> v.status()==DartFinancialImportStatus.FAILED).count();
        List<DisclosureEvidenceImportHistory> disclosureRuns=disclosureHistories.findRecentDisclosureImports(100);
        String latestDisclosure=disclosureRuns.stream().max(Comparator.comparing(DisclosureEvidenceImportHistory::requestedAt))
                .map(v->v.status().name()).orElse("NOT_RUN");
        int disclosureFailed=(int)disclosureRuns.stream().filter(v->v.status()==DisclosureEvidenceImportStatus.FAILED).count();
        int highDisclosure=(int)catalystEvidences.findRecent(500).stream()
                .filter(v->v.importance()==CatalystImportance.HIGH)
                .filter(v->v.evidenceType()==CatalystEvidenceType.DART_DISCLOSURE||v.evidenceType()==CatalystEvidenceType.KRX_DISCLOSURE).count();
        List<String> warnings = new ArrayList<>(); if (!dartProperties.isProviderEnabled()) warnings.add("DART_PROVIDER_DISABLED");
        if (missing > 0) warnings.add("DART_MAPPING_MISSING");
        if(!disclosureProperties.isEnabled())warnings.add("DISCLOSURE_PROVIDER_DISABLED");
        if(disclosureProperties.isEnabled()&&disclosureFailed>=2)warnings.add("DISCLOSURE_IMPORT_REPEATED_FAILURE");
        return new DartStatus(dartProperties.isProviderEnabled(), latestFinancial, latestCorp, missing, failed,
                disclosureProperties.isEnabled(),latestDisclosure,disclosureFailed,highDisclosure,warnings);
    }

    private ValuationStatus valuation(LocalDate date, List<Stock> active, List<SchedulerExecutionHistoryRecord> runs) {
        int generated=0, missingShares=0;
        for (Stock stock : active) {
            if (valuations.findLatestByStockCode(stock.stockCode(), date).isPresent()) generated++;
            if (shares.findLatestSharesByStockCode(stock.stockCode(), date).isEmpty()) missingShares++;
        }
        int insufficient = active.size()-generated;
        List<String> warnings = new ArrayList<>(); if (missingShares>0) warnings.add("SHARES_OUTSTANDING_MISSING");
        if (insufficient>0) warnings.add("VALUATION_DATA_INSUFFICIENT");
        return new ValuationStatus(latestStatus(runs, SchedulerName.VALUATION_AUTO_SNAPSHOT), generated, insufficient, missingShares, warnings);
    }

    private OperationalDashboardSummary.PaperTradingReportStatus paper(LocalDate date) {
        Optional<PaperTradingReportRun> optional = paperReports.findLatestRun(date);
        if (optional.isEmpty()) return new OperationalDashboardSummary.PaperTradingReportStatus(null, false, 0, null, null, 0, List.of("PAPER_TRADING_REPORT_MISSING"));
        PaperTradingReportRun run = optional.get(); List<PaperTradingReportResult> results = paperReports.findResults(run.id());
        int evaluated = run.winCount()+run.lossCount()+run.flatCount();
        BigDecimal winRate = evaluated==0 ? BigDecimal.ZERO : BigDecimal.valueOf(run.winCount()*100L)
                .divide(BigDecimal.valueOf(evaluated), 2, RoundingMode.HALF_UP);
        int insufficient=(int)results.stream().filter(v -> v.resultStatus()==PaperTradingResultStatus.DATA_INSUFFICIENT).count();
        return new OperationalDashboardSummary.PaperTradingReportStatus(run.id(), run.status()==seokhoon.trade.domain.research.PaperTradingReportStatus.COMPLETED,
                run.totalCandidates(), winRate, run.averageReturnRate(), insufficient,
                insufficient>0 ? List.of("PAPER_TRADING_DATA_INSUFFICIENT") : List.of());
    }

    private OperationalDashboardSummary.ReplayBacktestStatus replay(LocalDate date) {
        return replayBacktests.findLatestRun().map(run -> {
            int evaluated=run.winCount()+run.lossCount();
            BigDecimal rate=evaluated==0 ? BigDecimal.ZERO : BigDecimal.valueOf(run.winCount()*100L)
                    .divide(BigDecimal.valueOf(evaluated),2,RoundingMode.HALF_UP);
            boolean stale=run.toDate()!=null && run.toDate().isBefore(date.minusDays(30));
            return new OperationalDashboardSummary.ReplayBacktestStatus(run.id(), value(run.strategy()), value(run.status()), rate,
                    run.averageReturnRate(), stale ? List.of("REPLAY_BACKTEST_STALE") : List.of());
        }).orElseGet(() -> new OperationalDashboardSummary.ReplayBacktestStatus(null, null, "NOT_RUN", null, null, List.of("REPLAY_BACKTEST_MISSING")));
    }

    private SchedulerStatus scheduler(List<SchedulerExecutionHistoryRecord> runs) {
        int success=countRuns(runs,SchedulerExecutionStatus.SUCCEEDED), failed=countRuns(runs,SchedulerExecutionStatus.FAILED);
        List<SchedulerFailure> failures=runs.stream().filter(v->v.status()==SchedulerExecutionStatus.FAILED)
                .sorted(Comparator.comparing(SchedulerExecutionHistoryRecord::startedAt).reversed()).limit(10)
                .map(v->new SchedulerFailure(v.schedulerName().name(),v.failureReason(),v.finishedAt())).toList();
        return new SchedulerStatus(runs.size(),success,failed,countRuns(runs,SchedulerExecutionStatus.SKIPPED),failures,
                failed>0 ? List.of("SCHEDULER_FAILURES_PRESENT") : List.of());
    }

    private KisTokenStatus token() {
        List<KisTokenUseCases.KisTokenStatus> values=tokens.statuses();
        Map<KisEnvironment,KisTokenUseCases.KisTokenStatus> byEnv=values.stream().collect(Collectors.toMap(
                KisTokenUseCases.KisTokenStatus::environment,Function.identity(),(a,b)->a));
        String cache=values.isEmpty()?"UNAVAILABLE":values.get(0).cacheMode().name();
        String real=tokenStatus(byEnv.get(KisEnvironment.REAL)), demo=tokenStatus(byEnv.get(KisEnvironment.DEMO));
        boolean refreshing=values.stream().anyMatch(KisTokenUseCases.KisTokenStatus::refreshInProgress);
        return new KisTokenStatus(cache,real,demo,refreshing,"UNAVAILABLE".equals(cache)?List.of("KIS_TOKEN_STATUS_UNAVAILABLE"):List.of());
    }

    private LiveTradingReadinessStatus live() {
        LiveTradingReadinessReport report=liveReadiness.checkReadiness();
        return new LiveTradingReadinessStatus(report.ready(),report.liveTradingEnabled(),report.kisTradingEnabled(),
                report.killSwitchEnabled(),report.blockingReasons(),report.warnings());
    }

    private String tokenStatus(KisTokenUseCases.KisTokenStatus status) {
        if(status==null)return "UNAVAILABLE"; if(!status.tokenPresent())return "MISSING";
        return status.secondsToExpire()<=0 ? "INVALID" : status.secondsToExpire()<3600 ? "EXPIRING" : "VALID";
    }
    private static int selected(List<SchedulerExecutionHistoryRecord> runs,SchedulerName name){return runs.stream().filter(v->v.schedulerName()==name)
            .max(Comparator.comparing(SchedulerExecutionHistoryRecord::startedAt)).map(v->v.selectedCount()==null?0:v.selectedCount()).orElse(0);}
    private static String latestStatus(List<SchedulerExecutionHistoryRecord> runs,SchedulerName name){return latestStatus(runs.stream().filter(v->v.schedulerName()==name).toList());}
    private static String latestStatus(List<SchedulerExecutionHistoryRecord> runs){return runs.stream().max(Comparator.comparing(SchedulerExecutionHistoryRecord::startedAt)).map(v->v.status().name()).orElse("NOT_RUN");}
    private static int count(List<StockSupplyDemandSnapshot> values,SupplyDemandStatus status){return (int)values.stream().filter(v->v.status()==status).count();}
    private static int countEarnings(List<EarningsAnalysisSnapshot> values,EarningsAnalysisStatus status){return (int)values.stream().filter(v->v.status()==status).count();}
    private static int countRuns(List<SchedulerExecutionHistoryRecord> values,SchedulerExecutionStatus status){return (int)values.stream().filter(v->v.status()==status).count();}
    private static String value(Object value){return value==null?"UNAVAILABLE":value.toString();}
    @SafeVarargs private static void addWarnings(List<String> target,List<String>... sources){for(List<String>s:sources)target.addAll(s);}
    private static List<String> distinct(List<String> values){return values.stream().distinct().toList();}
}
