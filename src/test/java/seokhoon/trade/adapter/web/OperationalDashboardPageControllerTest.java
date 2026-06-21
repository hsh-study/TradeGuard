package seokhoon.trade.adapter.web;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import seokhoon.trade.application.port.in.GetOperationalDashboardUseCase;
import seokhoon.trade.application.port.in.GetBootReadinessReportUseCase;
import seokhoon.trade.application.port.in.OperationalDashboardSummary;
import seokhoon.trade.application.port.in.RequestMockOrderUseCase;
import seokhoon.trade.application.port.out.BrokerPort;
import seokhoon.trade.domain.operations.BootReadinessReport;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OperationalDashboardPageControllerTest {
    private static final LocalDate DATE = LocalDate.of(2026, 6, 15);

    @Test void rendersTodayDashboardWithPrioritySections() throws Exception {
        GetOperationalDashboardUseCase useCase = mock(GetOperationalDashboardUseCase.class);
        GetBootReadinessReportUseCase boot = emptyBootReadiness();
        when(useCase.getTodayDashboard()).thenReturn(summary());
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new OperationalDashboardPageController(useCase,boot)).build();

        mvc.perform(get("/operations/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(containsString("상태: BLOCKED")))
                .andExpect(content().string(containsString("Blocking Issues")))
                .andExpect(content().string(containsString("MORNING_NOTE_NOT_GENERATED")))
                .andExpect(content().string(containsString("Warnings")))
                .andExpect(content().string(containsString("DISCORD_DISABLED")))
                .andExpect(content().string(containsString("Recommended Actions")))
                .andExpect(content().string(containsString("Run Morning Note")))
                .andExpect(content().string(containsString("/api/operations/boot-readiness")));

        verify(useCase).getTodayDashboard();
    }

    @Test void delegatesBaseDateAndDoesNotTouchOrderBoundaries() throws Exception {
        GetOperationalDashboardUseCase useCase = mock(GetOperationalDashboardUseCase.class);
        GetBootReadinessReportUseCase boot = emptyBootReadiness();
        BrokerPort broker = mock(BrokerPort.class);
        RequestMockOrderUseCase orders = mock(RequestMockOrderUseCase.class);
        when(useCase.getDashboard(DATE)).thenReturn(summary());
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new OperationalDashboardPageController(useCase,boot)).build();

        mvc.perform(get("/operations/dashboard").param("baseDate", DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"2026-06-15\"")));

        verify(useCase).getDashboard(DATE);
        verifyNoInteractions(broker, orders);
    }

    @Test void excludesSensitiveValuesAndUnapprovedFailureDetails() throws Exception {
        GetOperationalDashboardUseCase useCase = mock(GetOperationalDashboardUseCase.class);
        GetBootReadinessReportUseCase boot = emptyBootReadiness();
        when(useCase.getTodayDashboard()).thenReturn(summary());
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new OperationalDashboardPageController(useCase,boot)).build();

        mvc.perform(get("/operations/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("secret-token-value"))))
                .andExpect(content().string(not(containsString("123-45-67890"))))
                .andExpect(content().string(not(containsString("https://discord.example/secret"))))
                .andExpect(content().string(not(containsString("receipt-secret"))))
                .andExpect(content().string(not(containsString("external-provider-secret"))));
    }

    @Test void showsBlockedBootReadinessAction() throws Exception {
        GetOperationalDashboardUseCase dashboard=mock(GetOperationalDashboardUseCase.class);
        when(dashboard.getTodayDashboard()).thenReturn(summary());
        GetBootReadinessReportUseCase boot=mock(GetBootReadinessReportUseCase.class);
        BootReadinessReport report=mock(BootReadinessReport.class);
        when(report.overallStatus()).thenReturn(BootReadinessReport.OverallStatus.BLOCKED);
        when(report.checkedAt()).thenReturn(Instant.parse("2026-06-15T03:00:00Z"));
        when(boot.getLatestReport()).thenReturn(Optional.of(report));
        MockMvc mvc=MockMvcBuilders.standaloneSetup(new OperationalDashboardPageController(dashboard,boot)).build();

        mvc.perform(get("/operations/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Boot readiness: BLOCKED")))
                .andExpect(content().string(containsString("Review blocked Boot Readiness Report")));
    }

    @Test void separatesTradingAndResearchViews() throws Exception {
        GetOperationalDashboardUseCase dashboard=mock(GetOperationalDashboardUseCase.class);
        when(dashboard.getTodayDashboard()).thenReturn(summary());
        MockMvc mvc=MockMvcBuilders.standaloneSetup(
                new OperationalDashboardPageController(dashboard,emptyBootReadiness())).build();

        mvc.perform(get("/operations/dashboard").param("view","trading"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("투자 운영 메뉴")))
                .andExpect(content().string(containsString("/operations/portfolio")))
                .andExpect(content().string(containsString("/operations/trading")))
                .andExpect(content().string(containsString("/api/live-orders/open")))
                .andExpect(content().string(not(containsString("Earnings consensus"))));

        mvc.perform(get("/operations/dashboard").param("view","research"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Earnings consensus")))
                .andExpect(content().string(containsString("News")))
                .andExpect(content().string(containsString("NEWS_PROVIDER_DISABLED")))
                .andExpect(content().string(not(containsString("주문·포지션 조회 API"))));
    }

    private static GetBootReadinessReportUseCase emptyBootReadiness() {
        GetBootReadinessReportUseCase useCase=mock(GetBootReadinessReportUseCase.class);
        when(useCase.getLatestReport()).thenReturn(Optional.empty());
        return useCase;
    }

    private static OperationalDashboardSummary summary() {
        List<String> none = List.of();
        return new OperationalDashboardSummary(
                DATE,
                new OperationalDashboardSummary.MarketDateStatus(DATE,true,DATE.minusDays(3),DATE.plusDays(1),"CONFIGURED_MARKET_CALENDAR",none),
                new OperationalDashboardSummary.MorningNoteStatus(false,DATE,0,0,null,false,List.of("DISCORD_DISABLED")),
                new OperationalDashboardSummary.EarlyMarketStatus(2,1,1,1,"SUCCEEDED","SUCCEEDED",none),
                new OperationalDashboardSummary.ClosingBetStatus(3,1,"SUCCEEDED","SUCCEEDED",none),
                new OperationalDashboardSummary.InvestorFlowStatus(true,true,"KRW",true,"SUCCEEDED","SUCCEEDED","SUCCEEDED",1,0,none),
                new OperationalDashboardSummary.EarningsStatus(2,1,0,1,1,0,none),
                new OperationalDashboardSummary.DartStatus(true,"SUCCEEDED","SUCCEEDED",0,0,true,"SUCCEEDED",0,1,none),
                new OperationalDashboardSummary.ConsensusStatus(2,2,0,0,none),
                new OperationalDashboardSummary.ValuationStatus("SUCCEEDED",2,0,0,none),
                new OperationalDashboardSummary.PaperTradingReportStatus(1L,true,3,new BigDecimal("66.67"),new BigDecimal("1.25"),0,none),
                new OperationalDashboardSummary.ReplayBacktestStatus(2L,"CLOSING_BET","COMPLETED",new BigDecimal("60"),new BigDecimal("0.80"),none),
                new OperationalDashboardSummary.SchedulerStatus(4,3,1,0,List.of(
                        new OperationalDashboardSummary.SchedulerFailure("MORNING_NOTE","token=secret-token-value accountNo=123-45-67890 webhook=https://discord.example/secret receiptNo=receipt-secret providerName=external-provider-secret",Instant.parse("2026-06-15T01:00:00Z"))),none),
                new OperationalDashboardSummary.KisTokenStatus("DB","VALID","VALID",false,none),
                new OperationalDashboardSummary.LiveTradingReadinessStatus(false,false,false,true,List.of("LIVE_TRADING_DISABLED"),none),
                List.of("MORNING_NOTE_NOT_GENERATED"),
                List.of("DISCORD_DISABLED", "sourceUrl=https://example.com/private"),
                List.of("Run Morning Note"), Instant.parse("2026-06-15T02:00:00Z"));
    }
}
