package seokhoon.trade.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import seokhoon.trade.application.port.in.*;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.config.DartProperties;
import seokhoon.trade.domain.kis.*;
import seokhoon.trade.domain.market.*;
import seokhoon.trade.domain.order.LiveTradingReadinessReport;

import java.time.*;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OperationalDashboardServiceTest {
    private static final LocalDate DATE = LocalDate.of(2026, 6, 15);
    @Mock LoadMarketCalendarUseCase calendar; @Mock MorningNotePort notes;
    @Mock EarlyMarketDataCapturePort captures; @Mock EarlyMarketFollowUpResultPort followUps;
    @Mock EarlyMarketPerformancePort performances; @Mock SchedulerExecutionHistoryPort schedulers;
    @Mock GetInvestorFlowReadinessUseCase investorReadiness; @Mock SupplyDemandSnapshotPort supplyDemand;
    @Mock EarningsAnalysisPort earnings; @Mock EarningsEventPort earningsEvents;
    @Mock DartFinancialImportHistoryPort dartFinancials; @Mock DartCorpCodeImportHistoryPort dartCorpCodes;
    @Mock DartCorpMappingPort dartMappings; @Mock StockPort stocks; @Mock ValuationSnapshotPort valuations;
    @Mock StockSectorMappingPort stockSectors;
    @Mock SharesOutstandingSnapshotPort shares; @Mock PaperTradingReportPort papers;
    @Mock ReplayBacktestPort replays; @Mock KisTokenUseCases.ManageKisTokenUseCase tokens;
    @Mock LiveTradingReadinessUseCase liveReadiness;
    private OperationalDashboardService service;

    @BeforeEach void setUp() {
        when(calendar.load(DATE)).thenReturn(new MarketCalendarView(DATE, true, DATE.minusDays(3), DATE.plusDays(1)));
        when(captures.findCaptures(DATE)).thenReturn(List.of()); when(followUps.findByTradeDate(DATE)).thenReturn(List.of());
        when(performances.findByTradeDate(DATE)).thenReturn(List.of()); when(schedulers.find(DATE,null,null)).thenReturn(List.of());
        when(supplyDemand.findByTradeDate(DATE)).thenReturn(List.of()); when(earnings.findByBaseDate(DATE)).thenReturn(List.of());
        when(earningsEvents.findByStatusAndExpectedAnnouncementDateBetween(any(),any(),any())).thenReturn(List.of());
        when(dartCorpCodes.findAllCorpCodeImports()).thenReturn(List.of()); when(dartMappings.findAll()).thenReturn(List.of());
        when(stocks.findAll()).thenReturn(List.of()); when(tokens.statuses()).thenReturn(List.of(
                new KisTokenUseCases.KisTokenStatus(KisTokenCacheMode.DB, KisEnvironment.DEMO, true,
                        Instant.parse("2026-06-15T10:00:00Z"), 7200, DATE, false)));
        InvestorFlowReadiness readiness = new InvestorFlowReadiness(true,"KIS", KisInvestorFlowAmountUnit.KRW,
                false,false,false,true,false,20,null,null,null,false,
                List.of("AMOUNT_UNIT_UNVERIFIED"),List.of(),List.of());
        when(investorReadiness.getReadiness()).thenReturn(readiness);
        LiveTradingReadinessReport live = mock(LiveTradingReadinessReport.class);
        when(live.warnings()).thenReturn(List.of()); when(live.blockingReasons()).thenReturn(List.of());
        when(liveReadiness.checkReadiness()).thenReturn(live);
        service = new OperationalDashboardService(calendar,notes,captures,followUps,performances,schedulers,
                investorReadiness,supplyDemand,earnings,earningsEvents,dartFinancials,dartCorpCodes,dartMappings,
                new DartProperties(),stocks,stockSectors,valuations,shares,papers,replays,tokens,liveReadiness,
                "",Clock.fixed(Instant.parse("2026-06-15T00:00:00Z"),ZoneOffset.UTC));
    }

    @Test void reportsRequiredBlockingIssuesAndActionsWithoutCallingProviders() {
        OperationalDashboardSummary result = service.getDashboard(DATE);

        assertThat(result.blockingIssues()).contains("MORNING_NOTE_NOT_GENERATED",
                "INVESTOR_FLOW_AMOUNT_UNIT_UNVERIFIED", "PAPER_TRADING_REPORT_NOT_GENERATED");
        assertThat(result.recommendedActions()).contains("Run Morning Note",
                "Verify KIS investor flow amount unit", "Generate Paper Trading Report");
        assertThat(result.morningNoteStatus().discordEnabled()).isFalse();
        verifyNoInteractions(dartFinancials, valuations, shares);
    }

    @Test void usesSeoulDateForTodayDashboard() {
        service.getTodayDashboard();
        verify(calendar).load(DATE);
    }
}
