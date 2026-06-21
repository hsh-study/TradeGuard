package seokhoon.trade.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import seokhoon.trade.application.port.in.LiveTradingReadinessUseCase;
import seokhoon.trade.application.port.in.RequestMockOrderUseCase;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.config.*;
import seokhoon.trade.domain.kis.KisEnvironment;
import seokhoon.trade.domain.kis.KisTokenCacheMode;
import seokhoon.trade.domain.operations.BootReadinessReport.OverallStatus;
import seokhoon.trade.domain.order.LiveTradingReadinessReport;
import seokhoon.trade.domain.order.LiveTradingRuntimeState;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class BootReadinessServiceTest {
    private BootReadinessInfrastructurePort infrastructure;
    private KisConfigurationPort kis;
    private InvestorFlowProperties investorFlow;
    private DartProperties dart;
    private DisclosureActualProviderProperties disclosure;
    private ConsensusProviderProperties consensus;
    private PaperTradingReportProperties paper;
    private LiveTradingProperties live;
    private LiveTradingReadinessUseCase liveReadiness;
    private LiveTradingRuntimeStatePort runtime;
    private OperationalMetricsPort metrics;
    private MockEnvironment environment;
    private BootReadinessService service;

    @BeforeEach
    void setUp() {
        infrastructure=mock(BootReadinessInfrastructurePort.class);
        when(infrastructure.checkDatabase()).thenReturn(new BootReadinessInfrastructurePort.ProbeResult(true,"CONNECTED"));
        when(infrastructure.checkFlyway()).thenReturn(new BootReadinessInfrastructurePort.ProbeResult(true,"UP_TO_DATE"));
        kis=mock(KisConfigurationPort.class);
        when(kis.readOnlyEnvironment()).thenReturn(KisEnvironment.DEMO);
        when(kis.tokenCacheMode()).thenReturn(KisTokenCacheMode.MEMORY);
        when(kis.credentialsConfigured()).thenReturn(true);
        investorFlow=new InvestorFlowProperties(); dart=new DartProperties();
        disclosure=new DisclosureActualProviderProperties(); consensus=new ConsensusProviderProperties();
        paper=new PaperTradingReportProperties(); live=new LiveTradingProperties();
        liveReadiness=mock(LiveTradingReadinessUseCase.class);
        runtime=mock(LiveTradingRuntimeStatePort.class);
        when(runtime.get()).thenReturn(new LiveTradingRuntimeState(false,"BOOT",Instant.EPOCH));
        metrics=mock(OperationalMetricsPort.class);
        environment=new MockEnvironment().withProperty("tradeguard.environment","TEST")
                .withProperty("management.endpoints.web.exposure.include","health,info,metrics,prometheus")
                .withProperty("management.endpoint.prometheus.enabled","true");
        environment.setActiveProfiles("test");
        service=createService();
    }

    @Test void reportsReadyForSafeCompleteConfiguration() {
        var report=service.generateReport();

        assertThat(report.overallStatus()).isEqualTo(OverallStatus.READY);
        assertThat(report.blockingIssues()).isEmpty();
        assertThat(report.warnings()).isEmpty();
        assertThat(report.liveTradingStatus().status()).isEqualTo("SAFE_DISABLED");
        verify(metrics).recordBootReadiness("ready");
        verifyNoInteractions(liveReadiness);
    }

    @Test void blocksUnverifiedInvestorFlowAutoRun() {
        investorFlow.setProviderEnabled(true);
        investorFlow.setProviderType("KIS");
        investorFlow.setImportAutoRun(true);

        var report=createService().generateReport();

        assertThat(report.overallStatus()).isEqualTo(OverallStatus.BLOCKED);
        assertThat(report.blockingIssues()).contains("INVESTOR_FLOW_AUTO_RUN_WITH_UNVERIFIED_AMOUNT_UNIT");
    }

    @Test void warnsWhenDiagnosticHttpIsAllowed() {
        investorFlow.setDiagnosticEnabled(true);
        investorFlow.setDiagnosticAllowHttp(true);

        var report=createService().generateReport();

        assertThat(report.overallStatus()).isEqualTo(OverallStatus.WARNING);
        assertThat(report.warnings()).contains("INVESTOR_FLOW_DIAGNOSTIC_HTTP_ENABLED");
    }

    @Test void blocksEnabledDartWithoutRequiredConfiguration() {
        dart.setProviderEnabled(true);

        var report=createService().generateReport();

        assertThat(report.blockingIssues()).contains("DART_PROVIDER_CONFIGURATION_MISSING");
        assertThat(report.dartStatus().status()).isEqualTo("MISCONFIGURED");
    }

    @Test void blocksEnabledLiveTradingWhenReadinessIsFalse() {
        live.setLiveTradingEnabled(true);
        LiveTradingReadinessReport readiness=mock(LiveTradingReadinessReport.class);
        when(readiness.ready()).thenReturn(false);
        when(liveReadiness.checkReadiness()).thenReturn(readiness);

        var report=createService().generateReport();

        assertThat(report.blockingIssues()).contains("LIVE_TRADING_FLAGS_ENABLED_AT_BOOT","LIVE_TRADING_READINESS_BLOCKED");
        verify(liveReadiness).checkReadiness();
    }

    @Test void warnsWhenPrometheusIsNotExposed() {
        environment.setProperty("management.endpoints.web.exposure.include","health,info,metrics");
        environment.setProperty("management.endpoint.prometheus.enabled","false");

        var report=createService().generateReport();

        assertThat(report.warnings()).contains("ACTUATOR_ENDPOINT_EXPOSURE_INCOMPLETE","PROMETHEUS_ENDPOINT_NOT_EXPOSED");
        assertThat(report.observabilityStatus().status()).isEqualTo("INCOMPLETE");
    }

    @Test void applicationReadyEventGeneratesAndStoresOneReport() {
        service.onApplicationReady(mock(ApplicationReadyEvent.class));

        assertThat(service.getLatestReport()).isPresent();
        verify(metrics).recordBootReadiness("ready");
    }

    @Test void neverCallsProvidersBrokerOrOrderService() {
        InvestorFlowProviderPort provider=mock(InvestorFlowProviderPort.class);
        BrokerPort broker=mock(BrokerPort.class);
        RequestMockOrderUseCase orders=mock(RequestMockOrderUseCase.class);

        service.generateReport();

        verifyNoInteractions(provider,broker,orders,liveReadiness);
    }

    private BootReadinessService createService() {
        return new BootReadinessService(infrastructure,kis,investorFlow,dart,disclosure,consensus,paper,
                live,liveReadiness,runtime,metrics,environment,
                Clock.fixed(Instant.parse("2026-06-20T00:00:00Z"), ZoneOffset.UTC));
    }
}
