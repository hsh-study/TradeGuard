package seokhoon.trade.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.in.MarketCalendarSyncResult;
import seokhoon.trade.application.port.in.SyncMarketCalendarUseCase;
import seokhoon.trade.application.port.out.CorrelationIdProvider;
import seokhoon.trade.application.port.out.MarketCalendarDayPort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.application.port.out.SchedulerExecutionHistoryPort;
import seokhoon.trade.domain.scheduler.SchedulerExecutionStatus;
import seokhoon.trade.domain.scheduler.SchedulerName;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class MarketCalendarSyncScheduler {
    private static final Logger log =
            LoggerFactory.getLogger(MarketCalendarSyncScheduler.class);
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final SyncMarketCalendarUseCase syncUseCase;
    private final MarketCalendarDayPort calendarDayPort;
    private final SchedulerExecutionHistoryPort historyPort;
    private final OperationalMetricsPort metricsPort;
    private final CorrelationIdProvider correlationIdProvider;
    private final Clock clock;

    @Autowired
    public MarketCalendarSyncScheduler(
            SyncMarketCalendarUseCase syncUseCase,
            MarketCalendarDayPort calendarDayPort,
            SchedulerExecutionHistoryPort historyPort,
            OperationalMetricsPort metricsPort,
            CorrelationIdProvider correlationIdProvider
    ) {
        this(
                syncUseCase,
                calendarDayPort,
                historyPort,
                metricsPort,
                correlationIdProvider,
                Clock.system(SEOUL)
        );
    }

    MarketCalendarSyncScheduler(
            SyncMarketCalendarUseCase syncUseCase,
            MarketCalendarDayPort calendarDayPort,
            SchedulerExecutionHistoryPort historyPort,
            OperationalMetricsPort metricsPort,
            CorrelationIdProvider correlationIdProvider,
            Clock clock
    ) {
        this.syncUseCase = syncUseCase;
        this.calendarDayPort = calendarDayPort;
        this.historyPort = historyPort;
        this.metricsPort = metricsPort;
        this.correlationIdProvider = correlationIdProvider;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void syncMissingYears() {
        String correlationId = correlationIdProvider.newCorrelationId();
        MDC.put("correlationId", correlationId);
        try {
            execute(correlationId);
        } finally {
            MDC.remove("correlationId");
        }
    }

    private void execute(String correlationId) {
        LocalDate today = LocalDate.now(clock);
        int currentYear = today.getYear();
        boolean currentExists = calendarDayPort.existsByYear(currentYear);
        boolean nextExists = calendarDayPort.existsByYear(currentYear + 1);
        if (currentExists && nextExists) {
            historyPort.markSkipped(
                    SchedulerName.MARKET_CALENDAR_SYNC,
                    today,
                    "CALENDAR_YEARS_ALREADY_EXIST",
                    correlationId,
                    Instant.now(clock)
            );
            metricsPort.recordSchedulerExecution(
                    SchedulerName.MARKET_CALENDAR_SYNC,
                    SchedulerExecutionStatus.SKIPPED
            );
            return;
        }

        long historyId = historyPort.saveStarted(
                SchedulerName.MARKET_CALENDAR_SYNC,
                today,
                correlationId,
                Instant.now(clock)
        );
        metricsPort.recordSchedulerExecution(
                SchedulerName.MARKET_CALENDAR_SYNC,
                SchedulerExecutionStatus.STARTED
        );
        try {
            int scannedCount = 0;
            int selectedCount = 0;
            if (!currentExists) {
                MarketCalendarSyncResult result = syncUseCase.syncYear(currentYear);
                scannedCount += result.syncedCount();
                selectedCount += result.syncedCount();
            }
            if (!nextExists) {
                MarketCalendarSyncResult result = syncUseCase.syncYear(currentYear + 1);
                scannedCount += result.syncedCount();
                selectedCount += result.syncedCount();
            }
            historyPort.markSucceeded(
                    historyId,
                    scannedCount,
                    selectedCount,
                    false,
                    Instant.now(clock)
            );
            metricsPort.recordSchedulerExecution(
                    SchedulerName.MARKET_CALENDAR_SYNC,
                    SchedulerExecutionStatus.SUCCEEDED
            );
            metricsPort.recordSchedulerSelected(
                    SchedulerName.MARKET_CALENDAR_SYNC,
                    selectedCount
            );
            log.atInfo()
                    .addKeyValue("currentYear", currentYear)
                    .addKeyValue("scannedCount", scannedCount)
                    .addKeyValue("selectedCount", selectedCount)
                    .log("Market calendar sync scheduler succeeded");
        } catch (RuntimeException exception) {
            historyPort.markFailed(
                    historyId,
                    failureReason(exception),
                    Instant.now(clock)
            );
            metricsPort.recordSchedulerExecution(
                    SchedulerName.MARKET_CALENDAR_SYNC,
                    SchedulerExecutionStatus.FAILED
            );
            throw exception;
        }
    }

    private static String failureReason(RuntimeException exception) {
        return exception.getClass().getSimpleName();
    }
}
