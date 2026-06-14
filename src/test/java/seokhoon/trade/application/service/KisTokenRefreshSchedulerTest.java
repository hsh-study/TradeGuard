package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.config.KisEnvironmentUsage;
import seokhoon.trade.domain.kis.KisEnvironment;
import seokhoon.trade.domain.scheduler.*;

import java.time.*;
import java.util.Set;

import static org.mockito.Mockito.*;

class KisTokenRefreshSchedulerTest {
    private static final Clock CLOCK=Clock.fixed(
            Instant.parse("2026-06-13T00:00:00Z"),ZoneOffset.UTC);

    @Test
    void refreshesOnlyEnabledEnvironmentsAndRecordsSuccess() {
        var provider=mock(KisAccessTokenProvider.class);
        var usage=mock(KisEnvironmentUsage.class);
        var histories=mock(SchedulerExecutionHistoryPort.class);
        var metrics=mock(OperationalMetricsPort.class);
        var correlations=mock(CorrelationIdProvider.class);
        when(usage.enabledEnvironments()).thenReturn(Set.of(
                KisEnvironment.REAL,KisEnvironment.DEMO));
        when(histories.saveStarted(any(),any(),any(),any()))
                .thenReturn(7L);
        var scheduler=new KisTokenRefreshScheduler(provider,usage,histories,
                metrics,correlations,CLOCK);

        scheduler.execute("correlation");

        verify(provider).refresh(KisEnvironment.REAL);
        verify(provider).refresh(KisEnvironment.DEMO);
        verify(histories).markSucceeded(7L,2,2,false,CLOCK.instant());
        verify(metrics).recordSchedulerExecution(
                SchedulerName.KIS_TOKEN_REFRESH,
                SchedulerExecutionStatus.SUCCEEDED);
    }

    @Test
    void recordsFailureWithoutTokenDetails() {
        var provider=mock(KisAccessTokenProvider.class);
        var usage=mock(KisEnvironmentUsage.class);
        var histories=mock(SchedulerExecutionHistoryPort.class);
        when(usage.enabledEnvironments()).thenReturn(
                Set.of(KisEnvironment.REAL));
        when(histories.saveStarted(any(),any(),any(),any()))
                .thenReturn(3L);
        doThrow(new IllegalStateException("secret-token"))
                .when(provider).refresh(KisEnvironment.REAL);
        var scheduler=new KisTokenRefreshScheduler(provider,usage,histories,
                OperationalMetricsPort.noop(),
                mock(CorrelationIdProvider.class),CLOCK);

        scheduler.execute("correlation");

        verify(histories).markFailed(3L,"IllegalStateException",
                CLOCK.instant());
    }
}
