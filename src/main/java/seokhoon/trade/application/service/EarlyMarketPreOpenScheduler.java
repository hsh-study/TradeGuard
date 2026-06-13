package seokhoon.trade.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.in.CaptureEarlyMarketPreOpenDataUseCase;
import seokhoon.trade.application.port.in.EarlyMarketDataCaptureResult;
import seokhoon.trade.application.port.in.ScanEarlyMarketPreOpenUseCase;
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
import java.util.List;

@Component
public class EarlyMarketPreOpenScheduler {
    private static final Logger log = LoggerFactory.getLogger(EarlyMarketPreOpenScheduler.class);
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final int DEFAULT_LIMIT = 10;

    private final ScanEarlyMarketPreOpenUseCase useCase;
    private final CaptureEarlyMarketPreOpenDataUseCase dataCaptureUseCase;
    private final MarketCalendarPort marketCalendarPort;
    private final SchedulerExecutionHistoryPort historyPort;
    private final OperationalMetricsPort metricsPort;
    private final CorrelationIdProvider correlationIdProvider;
    private final Clock clock;

    @Autowired
    public EarlyMarketPreOpenScheduler(
            ScanEarlyMarketPreOpenUseCase useCase,
            CaptureEarlyMarketPreOpenDataUseCase dataCaptureUseCase,
            MarketCalendarPort marketCalendarPort,
            SchedulerExecutionHistoryPort historyPort,
            OperationalMetricsPort metricsPort,
            CorrelationIdProvider correlationIdProvider
    ) {
        this(
                useCase,
                dataCaptureUseCase,
                marketCalendarPort,
                historyPort,
                metricsPort,
                correlationIdProvider,
                Clock.system(SEOUL)
        );
    }

    EarlyMarketPreOpenScheduler(
            ScanEarlyMarketPreOpenUseCase useCase,
            MarketCalendarPort marketCalendarPort,
            SchedulerExecutionHistoryPort historyPort,
            OperationalMetricsPort metricsPort,
            CorrelationIdProvider correlationIdProvider,
            Clock clock
    ) {
        this(
                useCase,
                tradeDate -> new EarlyMarketDataCaptureResult(
                        tradeDate,
                        List.of()
                ),
                marketCalendarPort,
                historyPort,
                metricsPort,
                correlationIdProvider,
                clock
        );
    }

    EarlyMarketPreOpenScheduler(
            ScanEarlyMarketPreOpenUseCase useCase,
            CaptureEarlyMarketPreOpenDataUseCase dataCaptureUseCase,
            MarketCalendarPort marketCalendarPort,
            SchedulerExecutionHistoryPort historyPort,
            OperationalMetricsPort metricsPort,
            CorrelationIdProvider correlationIdProvider,
            Clock clock
    ) {
        this.useCase = useCase;
        this.dataCaptureUseCase = dataCaptureUseCase;
        this.marketCalendarPort = marketCalendarPort;
        this.historyPort = historyPort;
        this.metricsPort = metricsPort;
        this.correlationIdProvider = correlationIdProvider;
        this.clock = clock;
    }

    @Scheduled(cron = "0 30 8 * * MON-FRI", zone = "Asia/Seoul")
    public void scanPreOpen() {
        String correlationId = correlationIdProvider.newCorrelationId();
        MDC.put("correlationId", correlationId);
        try {
            execute(correlationId);
        } finally {
            MDC.remove("correlationId");
        }
    }

    private void execute(String correlationId) {
        SchedulerName schedulerName = SchedulerName.EARLY_MARKET_PRE_OPEN_830;
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
            EarlyMarketDataCaptureRunner.run(
                    log,
                    schedulerName,
                    tradeDate,
                    () -> dataCaptureUseCase.capturePreOpen(tradeDate)
            );
            var result = useCase.scan(tradeDate, DEFAULT_LIMIT);
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
