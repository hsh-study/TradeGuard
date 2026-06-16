package seokhoon.trade.application.service;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.in.ResearchUseCases.SectorSnapshotGenerationResult;
import seokhoon.trade.application.port.in.ResearchUseCases.SectorUseCase;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.domain.scheduler.SchedulerExecutionStatus;
import seokhoon.trade.domain.scheduler.SchedulerName;

import java.time.*;

@Component
public class SectorDailySnapshotScheduler {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final SectorUseCase useCase;
    private final MarketCalendarPort calendarPort;
    private final SchedulerExecutionHistoryPort historyPort;
    private final OperationalMetricsPort metrics;
    private final CorrelationIdProvider correlationIds;
    private final Clock clock;

    @Autowired
    public SectorDailySnapshotScheduler(
            SectorUseCase useCase,
            MarketCalendarPort calendarPort,
            SchedulerExecutionHistoryPort historyPort,
            OperationalMetricsPort metrics,
            CorrelationIdProvider correlationIds
    ) {
        this(useCase, calendarPort, historyPort, metrics, correlationIds, Clock.system(SEOUL));
    }

    SectorDailySnapshotScheduler(
            SectorUseCase useCase,
            MarketCalendarPort calendarPort,
            SchedulerExecutionHistoryPort historyPort,
            OperationalMetricsPort metrics,
            CorrelationIdProvider correlationIds,
            Clock clock
    ) {
        this.useCase = useCase;
        this.calendarPort = calendarPort;
        this.historyPort = historyPort;
        this.metrics = metrics;
        this.correlationIds = correlationIds;
        this.clock = clock;
    }

    @Scheduled(cron = "0 5 8 * * MON-FRI", zone = "Asia/Seoul")
    public void generateSectorDailySnapshot() {
        String correlationId = correlationIds.newCorrelationId();
        MDC.put("correlationId", correlationId);
        try {
            execute(correlationId);
        } finally {
            MDC.remove("correlationId");
        }
    }

    private void execute(String correlationId) {
        LocalDate today = LocalDate.now(clock);
        if (!calendarPort.isTradingDay(today)) {
            historyPort.markSkipped(SchedulerName.SECTOR_DAILY_SNAPSHOT, today,
                    "NON_TRADING_DAY", correlationId, clock.instant());
            metrics.recordSchedulerExecution(
                    SchedulerName.SECTOR_DAILY_SNAPSHOT, SchedulerExecutionStatus.SKIPPED);
            return;
        }
        LocalDate targetDate = calendarPort.previousTradingDay(today);
        long historyId = historyPort.saveStarted(
                SchedulerName.SECTOR_DAILY_SNAPSHOT, targetDate, correlationId, clock.instant());
        metrics.recordSchedulerExecution(
                SchedulerName.SECTOR_DAILY_SNAPSHOT, SchedulerExecutionStatus.STARTED);
        try {
            SectorSnapshotGenerationResult result = useCase.generateSnapshots(targetDate);
            historyPort.markSucceeded(historyId, result.sectorCount(), result.generatedCount(),
                    false, clock.instant());
            metrics.recordSchedulerExecution(
                    SchedulerName.SECTOR_DAILY_SNAPSHOT, SchedulerExecutionStatus.SUCCEEDED);
            metrics.recordSchedulerSelected(SchedulerName.SECTOR_DAILY_SNAPSHOT, result.generatedCount());
            metrics.recordSchedulerNotification(SchedulerName.SECTOR_DAILY_SNAPSHOT, false);
        } catch (RuntimeException exception) {
            historyPort.markFailed(historyId, failureReason(exception), clock.instant());
            metrics.recordSchedulerExecution(
                    SchedulerName.SECTOR_DAILY_SNAPSHOT, SchedulerExecutionStatus.FAILED);
            throw exception;
        }
    }

    private static String failureReason(RuntimeException exception) {
        String message = exception.getMessage();
        String reason = exception.getClass().getSimpleName()
                + (message == null || message.isBlank() ? "" : ": " + message);
        return reason.length() <= 1000 ? reason : reason.substring(0, 1000);
    }
}
