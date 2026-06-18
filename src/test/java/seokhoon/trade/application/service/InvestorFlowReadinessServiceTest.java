package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.out.InvestorFlowImportHistoryPort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.application.port.out.SchedulerExecutionHistoryRecord;
import seokhoon.trade.application.port.out.SchedulerExecutionHistoryPort;
import seokhoon.trade.config.InvestorFlowProperties;
import seokhoon.trade.domain.market.InvestorFlowImportHistory;
import seokhoon.trade.domain.market.InvestorFlowImportScope;
import seokhoon.trade.domain.market.InvestorFlowImportStatus;
import seokhoon.trade.domain.market.InvestorFlowMarket;
import seokhoon.trade.domain.market.InvestorFlowProvider;
import seokhoon.trade.domain.market.KisInvestorFlowAmountUnit;
import seokhoon.trade.domain.scheduler.SchedulerExecutionStatus;
import seokhoon.trade.domain.scheduler.SchedulerName;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InvestorFlowReadinessServiceTest {
    @Test
    void providerDisabledIsReadyWithWarningOnly() {
        var result = service(new InvestorFlowProperties()).getReadiness();

        assertThat(result.ready()).isTrue();
        assertThat(result.blockingReasons()).isEmpty();
        assertThat(result.warnings()).contains("INVESTOR_FLOW_PROVIDER_DISABLED");
    }

    @Test
    void enabledProviderWithUnverifiedAmountUnitIsNotReady() {
        InvestorFlowProperties properties = enabled();

        var result = service(properties).getReadiness();

        assertThat(result.ready()).isFalse();
        assertThat(result.blockingReasons()).contains("AMOUNT_UNIT_UNVERIFIED");
    }

    @Test
    void enabledProviderWithKrwUnitIsReadyWhenNoFailuresExist() {
        InvestorFlowProperties properties = enabled();
        properties.setKisAmountUnit(KisInvestorFlowAmountUnit.KRW);

        var result = service(properties).getReadiness();

        assertThat(result.ready()).isTrue();
        assertThat(result.amountUnitVerified()).isTrue();
    }

    @Test
    void diagnosticModeProducesWarnings() {
        InvestorFlowProperties properties = enabled();
        properties.setKisAmountUnit(KisInvestorFlowAmountUnit.KRW);
        properties.setDiagnosticEnabled(true);
        properties.setDiagnosticAllowHttp(true);

        var result = service(properties).getReadiness();

        assertThat(result.warnings())
                .contains("DIAGNOSTIC_MODE_ENABLED", "DIAGNOSTIC_HTTP_ENABLED");
        assertThat(result.recommendedNextActions())
                .contains("Disable KIS investor flow diagnostic after verification");
    }

    @Test
    void autoRunWithUnverifiedUnitAddsExplicitBlockingReason() {
        InvestorFlowProperties properties = enabled();
        properties.setImportAutoRun(true);

        var result = service(properties).getReadiness();

        assertThat(result.blockingReasons())
                .contains("AMOUNT_UNIT_UNVERIFIED",
                        "AUTO_RUN_BLOCKED_BY_UNVERIFIED_AMOUNT_UNIT");
    }

    @Test
    void recentImportAndAnalysisFailuresAreReported() {
        InvestorFlowProperties properties = enabled();
        properties.setKisAmountUnit(KisInvestorFlowAmountUnit.KRW);
        InvestorFlowImportHistoryPort imports = mock(InvestorFlowImportHistoryPort.class);
        SchedulerExecutionHistoryPort schedulers = mock(SchedulerExecutionHistoryPort.class);
        when(imports.findRecent(null, 100)).thenReturn(List.of(
                history(InvestorFlowImportScope.STOCK, InvestorFlowImportStatus.FAILED),
                history(InvestorFlowImportScope.MARKET, InvestorFlowImportStatus.SUCCESS)));
        when(schedulers.find(null, SchedulerName.SUPPLY_DEMAND_ANALYSIS, null)).thenReturn(List.of(
                new SchedulerExecutionHistoryRecord(1L, SchedulerName.SUPPLY_DEMAND_ANALYSIS,
                        LocalDate.of(2026, 6, 17), SchedulerExecutionStatus.FAILED,
                        null, "analysis failed", 1, 0, false, "test", Instant.now(), Instant.now())));

        var result = new InvestorFlowReadinessService(properties, imports, schedulers,
                OperationalMetricsPort.noop()).getReadiness();

        assertThat(result.latestStockImportStatus()).isEqualTo(InvestorFlowImportStatus.FAILED);
        assertThat(result.latestMarketImportStatus()).isEqualTo(InvestorFlowImportStatus.SUCCESS);
        assertThat(result.latestSupplyDemandAnalysisStatus()).isEqualTo(SchedulerExecutionStatus.FAILED);
        assertThat(result.blockingReasons())
                .contains("LATEST_STOCK_IMPORT_FAILED", "LATEST_SUPPLY_DEMAND_ANALYSIS_FAILED");
    }

    @Test
    void hasNoProviderHttpOrderOrBrokerDependency() {
        assertThat(Arrays.stream(InvestorFlowReadinessService.class.getDeclaredConstructors())
                .flatMap(value -> Arrays.stream(value.getParameterTypes()))
                .map(Class::getName))
                .noneMatch(value -> value.contains("InvestorFlowProviderPort")
                        || value.contains("Http") || value.contains("Order")
                        || value.contains("Broker"));
    }

    private static InvestorFlowReadinessService service(InvestorFlowProperties properties) {
        InvestorFlowImportHistoryPort imports = mock(InvestorFlowImportHistoryPort.class);
        SchedulerExecutionHistoryPort schedulers = mock(SchedulerExecutionHistoryPort.class);
        when(imports.findRecent(null, 100)).thenReturn(List.of());
        when(schedulers.find(null, seokhoon.trade.domain.scheduler.SchedulerName.SUPPLY_DEMAND_ANALYSIS, null))
                .thenReturn(List.of());
        return new InvestorFlowReadinessService(properties, imports, schedulers,
                OperationalMetricsPort.noop());
    }

    private static InvestorFlowProperties enabled() {
        InvestorFlowProperties properties = new InvestorFlowProperties();
        properties.setProviderEnabled(true);
        return properties;
    }

    private static InvestorFlowImportHistory history(InvestorFlowImportScope scope,
            InvestorFlowImportStatus status) {
        return new InvestorFlowImportHistory(1L, scope,
                scope == InvestorFlowImportScope.STOCK ? "005930" : null,
                scope == InvestorFlowImportScope.MARKET ? InvestorFlowMarket.KOSPI : null,
                LocalDate.of(2026, 6, 17), InvestorFlowProvider.KIS, status, 0,
                status == InvestorFlowImportStatus.FAILED ? "failed" : null,
                Instant.now(), Instant.now());
    }
}
