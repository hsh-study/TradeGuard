package seokhoon.trade.application.service;

import org.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.config.KisEnvironmentUsage;
import seokhoon.trade.domain.kis.KisEnvironment;
import seokhoon.trade.domain.scheduler.*;

import java.time.*;
import java.util.Set;

@Component
public class KisTokenRefreshScheduler {
    private static final Logger log=LoggerFactory.getLogger(
            KisTokenRefreshScheduler.class);
    private static final ZoneId SEOUL=ZoneId.of("Asia/Seoul");

    private final KisAccessTokenProvider provider;
    private final KisEnvironmentUsage usage;
    private final SchedulerExecutionHistoryPort histories;
    private final OperationalMetricsPort metrics;
    private final CorrelationIdProvider correlations;
    private final Clock clock;

    @Autowired
    public KisTokenRefreshScheduler(
            KisAccessTokenProvider provider,
            KisEnvironmentUsage usage,
            SchedulerExecutionHistoryPort histories,
            OperationalMetricsPort metrics,
            CorrelationIdProvider correlations
    ) {
        this(provider,usage,histories,metrics,correlations,
                Clock.system(SEOUL));
    }

    KisTokenRefreshScheduler(
            KisAccessTokenProvider provider,
            KisEnvironmentUsage usage,
            SchedulerExecutionHistoryPort histories,
            OperationalMetricsPort metrics,
            CorrelationIdProvider correlations,
            Clock clock
    ) {
        this.provider=provider;
        this.usage=usage;
        this.histories=histories;
        this.metrics=metrics;
        this.correlations=correlations;
        this.clock=clock;
    }

    @Scheduled(cron="#{@kisProperties.tokenRefreshCron()}",
            zone="Asia/Seoul")
    public void refresh() {
        String correlationId=correlations.newCorrelationId();
        MDC.put("correlationId",correlationId);
        try {
            execute(correlationId);
        } finally {
            MDC.remove("correlationId");
        }
    }

    void execute(String correlationId) {
        LocalDate today=LocalDate.now(clock);
        Set<KisEnvironment> environments=usage.enabledEnvironments();
        if (environments.isEmpty()) {
            histories.markSkipped(SchedulerName.KIS_TOKEN_REFRESH,today,
                    "KIS_ENVIRONMENT_DISABLED",correlationId,clock.instant());
            metrics.recordSchedulerExecution(SchedulerName.KIS_TOKEN_REFRESH,
                    SchedulerExecutionStatus.SKIPPED);
            return;
        }
        long historyId=histories.saveStarted(
                SchedulerName.KIS_TOKEN_REFRESH,today,correlationId,
                clock.instant());
        metrics.recordSchedulerExecution(SchedulerName.KIS_TOKEN_REFRESH,
                SchedulerExecutionStatus.STARTED);
        try {
            for (KisEnvironment environment : environments) {
                provider.refresh(environment);
            }
            histories.markSucceeded(historyId,environments.size(),
                    environments.size(),false,clock.instant());
            metrics.recordSchedulerExecution(SchedulerName.KIS_TOKEN_REFRESH,
                    SchedulerExecutionStatus.SUCCEEDED);
            metrics.recordSchedulerSelected(SchedulerName.KIS_TOKEN_REFRESH,
                    environments.size());
            log.atInfo().addKeyValue("environmentCount",
                    environments.size()).log("KIS token refresh succeeded");
        } catch (RuntimeException exception) {
            histories.markFailed(historyId,
                    exception.getClass().getSimpleName(),clock.instant());
            metrics.recordSchedulerExecution(SchedulerName.KIS_TOKEN_REFRESH,
                    SchedulerExecutionStatus.FAILED);
            log.atWarn().addKeyValue("errorType",
                    exception.getClass().getSimpleName())
                    .log("KIS token refresh failed");
        }
    }
}
