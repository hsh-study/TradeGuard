package seokhoon.trade.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import seokhoon.trade.application.port.in.ScanClosingBetCandidatesUseCase;
import seokhoon.trade.application.port.out.MarketCalendarPort;
import seokhoon.trade.application.port.out.CorrelationIdProvider;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.application.port.out.SchedulerExecutionHistoryPort;
import seokhoon.trade.domain.scheduler.SchedulerExecutionStatus;
import seokhoon.trade.domain.scheduler.SchedulerName;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class ClosingBetCandidateScanScheduler {
    private static final Logger log = LoggerFactory.getLogger(ClosingBetCandidateScanScheduler.class);
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final int DEFAULT_LIMIT = 5;

    private final ScanClosingBetCandidatesUseCase scanClosingBetCandidatesUseCase;
    private final MarketCalendarPort marketCalendarPort;
    private final SchedulerExecutionHistoryPort historyPort;
    private final OperationalMetricsPort metricsPort;
    private final CorrelationIdProvider correlationIdProvider;
    private final Clock clock;

    @Autowired
    public ClosingBetCandidateScanScheduler(
            ScanClosingBetCandidatesUseCase scanClosingBetCandidatesUseCase,
            MarketCalendarPort marketCalendarPort,
            SchedulerExecutionHistoryPort historyPort,
            OperationalMetricsPort metricsPort,
            CorrelationIdProvider correlationIdProvider
    ) {
        this(
                scanClosingBetCandidatesUseCase,
                marketCalendarPort,
                historyPort,
                metricsPort,
                correlationIdProvider,
                Clock.system(SEOUL)
        );
    }

    ClosingBetCandidateScanScheduler(
            ScanClosingBetCandidatesUseCase scanClosingBetCandidatesUseCase,
            MarketCalendarPort marketCalendarPort,
            Clock clock
    ) {
        this(
                scanClosingBetCandidatesUseCase,
                marketCalendarPort,
                SchedulerExecutionHistoryPort.noop(),
                OperationalMetricsPort.noop(),
                CorrelationIdProvider.generated(),
                clock
        );
    }

    ClosingBetCandidateScanScheduler(
            ScanClosingBetCandidatesUseCase scanClosingBetCandidatesUseCase,
            MarketCalendarPort marketCalendarPort,
            SchedulerExecutionHistoryPort historyPort,
            Clock clock
    ) {
        this(
                scanClosingBetCandidatesUseCase,
                marketCalendarPort,
                historyPort,
                OperationalMetricsPort.noop(),
                CorrelationIdProvider.generated(),
                clock
        );
    }

    ClosingBetCandidateScanScheduler(
            ScanClosingBetCandidatesUseCase scanClosingBetCandidatesUseCase,
            MarketCalendarPort marketCalendarPort,
            SchedulerExecutionHistoryPort historyPort,
            OperationalMetricsPort metricsPort,
            Clock clock
    ) {
        this(
                scanClosingBetCandidatesUseCase,
                marketCalendarPort,
                historyPort,
                metricsPort,
                CorrelationIdProvider.generated(),
                clock
        );
    }

    ClosingBetCandidateScanScheduler(
            ScanClosingBetCandidatesUseCase scanClosingBetCandidatesUseCase,
            MarketCalendarPort marketCalendarPort,
            SchedulerExecutionHistoryPort historyPort,
            OperationalMetricsPort metricsPort,
            CorrelationIdProvider correlationIdProvider,
            Clock clock
    ) {
        this.scanClosingBetCandidatesUseCase = scanClosingBetCandidatesUseCase;
        this.marketCalendarPort = marketCalendarPort;
        this.historyPort = historyPort;
        this.metricsPort = metricsPort;
        this.correlationIdProvider = correlationIdProvider;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 14 * * MON-FRI", zone = "Asia/Seoul")
    public void scanAtMarketAfternoon() {
        String correlationId = correlationIdProvider.newCorrelationId();
        MDC.put("correlationId", correlationId);
        try {
            executeScheduledScan(correlationId);
        } finally {
            MDC.remove("correlationId");
        }
    }

    private void executeScheduledScan(String correlationId) {
        SchedulerName schedulerName = SchedulerName.CLOSING_BET_PRE_SCAN_14;
        LocalDate tradeDate = LocalDate.now(clock);
        if (!marketCalendarPort.isTradingDay(tradeDate)) {
            historyPort.markSkipped(
                    schedulerName,
                    tradeDate,
                    "NON_TRADING_DAY",
                    correlationId,
                    Instant.now(clock)
            );
            metricsPort.recordSchedulerExecution(
                    schedulerName,
                    SchedulerExecutionStatus.SKIPPED
            );
            log.atInfo()
                    .addKeyValue("schedulerName", schedulerName)
                    .addKeyValue("tradeDate", tradeDate)
                    .addKeyValue("status", SchedulerExecutionStatus.SKIPPED)
                    .addKeyValue("correlationId", correlationId)
                    .log("Scheduler execution skipped");
            return;
        }
        long historyId = historyPort.saveStarted(
                schedulerName,
                tradeDate,
                correlationId,
                Instant.now(clock)
        );
        metricsPort.recordSchedulerExecution(
                schedulerName,
                SchedulerExecutionStatus.STARTED
        );
        log.atInfo()
                .addKeyValue("schedulerName", schedulerName)
                .addKeyValue("tradeDate", tradeDate)
                .addKeyValue("status", SchedulerExecutionStatus.STARTED)
                .addKeyValue("correlationId", correlationId)
                .log("Scheduler execution started");
        try {
            var result = scanClosingBetCandidatesUseCase.scan(tradeDate, DEFAULT_LIMIT);
            historyPort.markSucceeded(
                    historyId,
                    result.scannedCount(),
                    result.selectedCount(),
                    result.briefingSent(),
                    Instant.now(clock)
            );
            metricsPort.recordSchedulerExecution(
                    schedulerName,
                    SchedulerExecutionStatus.SUCCEEDED
            );
            metricsPort.recordSchedulerSelected(schedulerName, result.selectedCount());
            metricsPort.recordSchedulerNotification(schedulerName, result.briefingSent());
            log.atInfo()
                    .addKeyValue("schedulerName", schedulerName)
                    .addKeyValue("tradeDate", tradeDate)
                    .addKeyValue("status", SchedulerExecutionStatus.SUCCEEDED)
                    .addKeyValue("scannedCount", result.scannedCount())
                    .addKeyValue("selectedCount", result.selectedCount())
                    .addKeyValue("notificationSent", result.briefingSent())
                    .addKeyValue("correlationId", correlationId)
                    .log("Scheduler execution succeeded");
        } catch (RuntimeException exception) {
            historyPort.markFailed(
                    historyId,
                    failureReason(exception),
                    Instant.now(clock)
            );
            metricsPort.recordSchedulerExecution(
                    schedulerName,
                    SchedulerExecutionStatus.FAILED
            );
            log.atError()
                    .addKeyValue("schedulerName", schedulerName)
                    .addKeyValue("tradeDate", tradeDate)
                    .addKeyValue("status", SchedulerExecutionStatus.FAILED)
                    .addKeyValue("correlationId", correlationId)
                    .setCause(exception)
                    .log("Scheduler execution failed");
            throw exception;
        }
    }

    private static String failureReason(RuntimeException exception) {
        String message = exception.getMessage();
        String reason = message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getClass().getSimpleName() + ": " + message;
        return reason.length() <= 1000 ? reason : reason.substring(0, 1000);
    }
}
