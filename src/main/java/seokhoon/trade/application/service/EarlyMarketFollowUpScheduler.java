package seokhoon.trade.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.in.CaptureEarlyMarketFollowUpDataUseCase;
import seokhoon.trade.application.port.in.EarlyMarketDataCaptureResult;
import seokhoon.trade.application.port.in.EarlyMarketFollowUpResult;
import seokhoon.trade.application.port.in.FollowUpEarlyMarketCandidatesUseCase;
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
public class EarlyMarketFollowUpScheduler {
    private static final Logger log = LoggerFactory.getLogger(EarlyMarketFollowUpScheduler.class);
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final FollowUpEarlyMarketCandidatesUseCase followUpUseCase;
    private final CaptureEarlyMarketFollowUpDataUseCase dataCaptureUseCase;
    private final MarketCalendarPort marketCalendarPort;
    private final SchedulerExecutionHistoryPort historyPort;
    private final OperationalMetricsPort metricsPort;
    private final CorrelationIdProvider correlationIdProvider;
    private final Clock clock;

    @Autowired
    public EarlyMarketFollowUpScheduler(
            FollowUpEarlyMarketCandidatesUseCase followUpUseCase,
            CaptureEarlyMarketFollowUpDataUseCase dataCaptureUseCase,
            MarketCalendarPort marketCalendarPort,
            SchedulerExecutionHistoryPort historyPort,
            OperationalMetricsPort metricsPort,
            CorrelationIdProvider correlationIdProvider
    ) {
        this(
                followUpUseCase,
                dataCaptureUseCase,
                marketCalendarPort,
                historyPort,
                metricsPort,
                correlationIdProvider,
                Clock.system(SEOUL)
        );
    }

    EarlyMarketFollowUpScheduler(
            FollowUpEarlyMarketCandidatesUseCase followUpUseCase,
            MarketCalendarPort marketCalendarPort,
            SchedulerExecutionHistoryPort historyPort,
            OperationalMetricsPort metricsPort,
            CorrelationIdProvider correlationIdProvider,
            Clock clock
    ) {
        this(
                followUpUseCase,
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

    EarlyMarketFollowUpScheduler(
            FollowUpEarlyMarketCandidatesUseCase followUpUseCase,
            CaptureEarlyMarketFollowUpDataUseCase dataCaptureUseCase,
            MarketCalendarPort marketCalendarPort,
            SchedulerExecutionHistoryPort historyPort,
            OperationalMetricsPort metricsPort,
            CorrelationIdProvider correlationIdProvider,
            Clock clock
    ) {
        this.followUpUseCase = followUpUseCase;
        this.dataCaptureUseCase = dataCaptureUseCase;
        this.marketCalendarPort = marketCalendarPort;
        this.historyPort = historyPort;
        this.metricsPort = metricsPort;
        this.correlationIdProvider = correlationIdProvider;
        this.clock = clock;
    }

    @Scheduled(cron = "0 20 9 * * MON-FRI", zone = "Asia/Seoul")
    public void followUpAfterOpening() {
        String correlationId = correlationIdProvider.newCorrelationId();
        MDC.put("correlationId", correlationId);
        try {
            execute(correlationId);
        } finally {
            MDC.remove("correlationId");
        }
    }

    private void execute(String correlationId) {
        SchedulerName schedulerName = SchedulerName.EARLY_MARKET_FOLLOW_UP_920;
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
            logResult(
                    tradeDate,
                    SchedulerExecutionStatus.SKIPPED,
                    0,
                    0,
                    0,
                    0,
                    false,
                    correlationId
            );
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
        logResult(
                tradeDate,
                SchedulerExecutionStatus.STARTED,
                0,
                0,
                0,
                0,
                false,
                correlationId
        );
        try {
            EarlyMarketDataCaptureRunner.run(
                    log,
                    schedulerName,
                    tradeDate,
                    () -> dataCaptureUseCase.captureFollowUp(tradeDate)
            );
            EarlyMarketFollowUpResult result = followUpUseCase.followUp(tradeDate);
            historyPort.markSucceeded(
                    historyId,
                    result.checkedCount(),
                    result.keepCount(),
                    result.briefingSent(),
                    Instant.now(clock)
            );
            metricsPort.recordSchedulerExecution(
                    schedulerName,
                    SchedulerExecutionStatus.SUCCEEDED
            );
            metricsPort.recordSchedulerSelected(schedulerName, result.keepCount());
            metricsPort.recordSchedulerNotification(
                    schedulerName,
                    result.briefingSent()
            );
            logResult(
                    tradeDate,
                    SchedulerExecutionStatus.SUCCEEDED,
                    result.checkedCount(),
                    result.keepCount(),
                    result.cautionCount(),
                    result.excludeCount(),
                    result.briefingSent(),
                    correlationId
            );
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
                    .addKeyValue("result", SchedulerExecutionStatus.FAILED)
                    .addKeyValue("correlationId", correlationId)
                    .setCause(exception)
                    .log("Early market follow-up scheduler failed");
            throw exception;
        }
    }

    private static void logResult(
            LocalDate tradeDate,
            SchedulerExecutionStatus result,
            int checkedCount,
            int keepCount,
            int cautionCount,
            int excludeCount,
            boolean notificationSent,
            String correlationId
    ) {
        log.atInfo()
                .addKeyValue("schedulerName", SchedulerName.EARLY_MARKET_FOLLOW_UP_920)
                .addKeyValue("tradeDate", tradeDate)
                .addKeyValue("checkedCount", checkedCount)
                .addKeyValue("keepCount", keepCount)
                .addKeyValue("cautionCount", cautionCount)
                .addKeyValue("excludeCount", excludeCount)
                .addKeyValue("notificationSent", notificationSent)
                .addKeyValue("result", result)
                .addKeyValue("correlationId", correlationId)
                .log("Early market follow-up scheduler status");
    }

    private static String failureReason(RuntimeException exception) {
        String message = exception.getMessage();
        String reason = message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getClass().getSimpleName() + ": " + message;
        return reason.length() <= 1000 ? reason : reason.substring(0, 1000);
    }
}
