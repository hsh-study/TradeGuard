package seokhoon.trade.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.in.CompressEarlyMarketOpeningUseCase;
import seokhoon.trade.application.port.out.CorrelationIdProvider;
import seokhoon.trade.application.port.out.MarketCalendarPort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.application.port.out.SchedulerExecutionHistoryPort;
import seokhoon.trade.domain.scheduler.SchedulerExecutionStatus;
import seokhoon.trade.domain.scheduler.SchedulerName;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class EarlyMarketOpeningScheduler {
    private static final Logger log = LoggerFactory.getLogger(EarlyMarketOpeningScheduler.class);
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final int DEFAULT_LIMIT = 3;

    private final CompressEarlyMarketOpeningUseCase useCase;
    private final MarketCalendarPort marketCalendarPort;
    private final SchedulerExecutionHistoryPort historyPort;
    private final OperationalMetricsPort metricsPort;
    private final CorrelationIdProvider correlationIdProvider;
    private final Clock clock;

    @Autowired
    public EarlyMarketOpeningScheduler(
            CompressEarlyMarketOpeningUseCase useCase,
            MarketCalendarPort marketCalendarPort,
            SchedulerExecutionHistoryPort historyPort,
            OperationalMetricsPort metricsPort,
            CorrelationIdProvider correlationIdProvider
    ) {
        this(
                useCase,
                marketCalendarPort,
                historyPort,
                metricsPort,
                correlationIdProvider,
                Clock.system(SEOUL)
        );
    }

    EarlyMarketOpeningScheduler(
            CompressEarlyMarketOpeningUseCase useCase,
            MarketCalendarPort marketCalendarPort,
            SchedulerExecutionHistoryPort historyPort,
            OperationalMetricsPort metricsPort,
            CorrelationIdProvider correlationIdProvider,
            Clock clock
    ) {
        this.useCase = useCase;
        this.marketCalendarPort = marketCalendarPort;
        this.historyPort = historyPort;
        this.metricsPort = metricsPort;
        this.correlationIdProvider = correlationIdProvider;
        this.clock = clock;
    }

    @Scheduled(cron = "0 5 9 * * MON-FRI", zone = "Asia/Seoul")
    public void compressAfterOpen() {
        String correlationId = correlationIdProvider.newCorrelationId();
        MDC.put("correlationId", correlationId);
        try {
            execute(correlationId);
        } finally {
            MDC.remove("correlationId");
        }
    }

    private void execute(String correlationId) {
        SchedulerName schedulerName = SchedulerName.EARLY_MARKET_OPENING_905;
        LocalDate tradeDate = LocalDate.now(clock);
        if (!marketCalendarPort.isTradingDay(tradeDate)) {
            historyPort.markSkipped(
                    schedulerName,
                    tradeDate,
                    "NON_TRADING_DAY",
                    correlationId,
                    Instant.now(clock)
            );
            metricsPort.recordSchedulerExecution(schedulerName, SchedulerExecutionStatus.SKIPPED);
            logResult(schedulerName, tradeDate, SchedulerExecutionStatus.SKIPPED, 0, 0, false, correlationId);
            return;
        }

        long historyId = historyPort.saveStarted(
                schedulerName,
                tradeDate,
                correlationId,
                Instant.now(clock)
        );
        metricsPort.recordSchedulerExecution(schedulerName, SchedulerExecutionStatus.STARTED);
        logResult(schedulerName, tradeDate, SchedulerExecutionStatus.STARTED, 0, 0, false, correlationId);
        try {
            var result = useCase.compress(tradeDate, DEFAULT_LIMIT);
            historyPort.markSucceeded(
                    historyId,
                    result.scannedCount(),
                    result.selectedCount(),
                    result.briefingSent(),
                    Instant.now(clock)
            );
            metricsPort.recordSchedulerExecution(schedulerName, SchedulerExecutionStatus.SUCCEEDED);
            metricsPort.recordSchedulerSelected(schedulerName, result.selectedCount());
            metricsPort.recordSchedulerNotification(schedulerName, result.briefingSent());
            logResult(
                    schedulerName,
                    tradeDate,
                    SchedulerExecutionStatus.SUCCEEDED,
                    result.scannedCount(),
                    result.selectedCount(),
                    result.briefingSent(),
                    correlationId
            );
        } catch (RuntimeException exception) {
            historyPort.markFailed(historyId, failureReason(exception), Instant.now(clock));
            metricsPort.recordSchedulerExecution(schedulerName, SchedulerExecutionStatus.FAILED);
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

    private static void logResult(
            SchedulerName schedulerName,
            LocalDate tradeDate,
            SchedulerExecutionStatus status,
            int scannedCount,
            int selectedCount,
            boolean notificationSent,
            String correlationId
    ) {
        log.atInfo()
                .addKeyValue("schedulerName", schedulerName)
                .addKeyValue("tradeDate", tradeDate)
                .addKeyValue("status", status)
                .addKeyValue("scannedCount", scannedCount)
                .addKeyValue("selectedCount", selectedCount)
                .addKeyValue("notificationSent", notificationSent)
                .addKeyValue("correlationId", correlationId)
                .log("Scheduler execution status");
    }

    private static String failureReason(RuntimeException exception) {
        String message = exception.getMessage();
        String reason = message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getClass().getSimpleName() + ": " + message;
        return reason.length() <= 1000 ? reason : reason.substring(0, 1000);
    }
}
