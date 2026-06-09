package seokhoon.trade.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import seokhoon.trade.application.port.in.ReviewClosingBetCandidatesUseCase;
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
public class ClosingBetFinalReviewScheduler {
    private static final Logger log = LoggerFactory.getLogger(ClosingBetFinalReviewScheduler.class);
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final int DEFAULT_LIMIT = 5;

    private final ReviewClosingBetCandidatesUseCase reviewClosingBetCandidatesUseCase;
    private final MarketCalendarPort marketCalendarPort;
    private final SchedulerExecutionHistoryPort historyPort;
    private final OperationalMetricsPort metricsPort;
    private final CorrelationIdProvider correlationIdProvider;
    private final Clock clock;

    @Autowired
    public ClosingBetFinalReviewScheduler(
            ReviewClosingBetCandidatesUseCase reviewClosingBetCandidatesUseCase,
            MarketCalendarPort marketCalendarPort,
            SchedulerExecutionHistoryPort historyPort,
            OperationalMetricsPort metricsPort,
            CorrelationIdProvider correlationIdProvider
    ) {
        this(
                reviewClosingBetCandidatesUseCase,
                marketCalendarPort,
                historyPort,
                metricsPort,
                correlationIdProvider,
                Clock.system(SEOUL)
        );
    }

    ClosingBetFinalReviewScheduler(
            ReviewClosingBetCandidatesUseCase reviewClosingBetCandidatesUseCase,
            MarketCalendarPort marketCalendarPort,
            Clock clock
    ) {
        this(
                reviewClosingBetCandidatesUseCase,
                marketCalendarPort,
                SchedulerExecutionHistoryPort.noop(),
                OperationalMetricsPort.noop(),
                CorrelationIdProvider.generated(),
                clock
        );
    }

    ClosingBetFinalReviewScheduler(
            ReviewClosingBetCandidatesUseCase reviewClosingBetCandidatesUseCase,
            MarketCalendarPort marketCalendarPort,
            SchedulerExecutionHistoryPort historyPort,
            Clock clock
    ) {
        this(
                reviewClosingBetCandidatesUseCase,
                marketCalendarPort,
                historyPort,
                OperationalMetricsPort.noop(),
                CorrelationIdProvider.generated(),
                clock
        );
    }

    ClosingBetFinalReviewScheduler(
            ReviewClosingBetCandidatesUseCase reviewClosingBetCandidatesUseCase,
            MarketCalendarPort marketCalendarPort,
            SchedulerExecutionHistoryPort historyPort,
            OperationalMetricsPort metricsPort,
            Clock clock
    ) {
        this(
                reviewClosingBetCandidatesUseCase,
                marketCalendarPort,
                historyPort,
                metricsPort,
                CorrelationIdProvider.generated(),
                clock
        );
    }

    ClosingBetFinalReviewScheduler(
            ReviewClosingBetCandidatesUseCase reviewClosingBetCandidatesUseCase,
            MarketCalendarPort marketCalendarPort,
            SchedulerExecutionHistoryPort historyPort,
            OperationalMetricsPort metricsPort,
            CorrelationIdProvider correlationIdProvider,
            Clock clock
    ) {
        this.reviewClosingBetCandidatesUseCase = reviewClosingBetCandidatesUseCase;
        this.marketCalendarPort = marketCalendarPort;
        this.historyPort = historyPort;
        this.metricsPort = metricsPort;
        this.correlationIdProvider = correlationIdProvider;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 15 * * MON-FRI", zone = "Asia/Seoul")
    public void reviewAtMarketLateAfternoon() {
        String correlationId = correlationIdProvider.newCorrelationId();
        MDC.put("correlationId", correlationId);
        try {
            executeScheduledReview(correlationId);
        } finally {
            MDC.remove("correlationId");
        }
    }

    private void executeScheduledReview(String correlationId) {
        SchedulerName schedulerName = SchedulerName.CLOSING_BET_FINAL_REVIEW_15;
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
            var result = reviewClosingBetCandidatesUseCase.review(tradeDate, DEFAULT_LIMIT);
            historyPort.markSucceeded(
                    historyId,
                    result.reviewedCount(),
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
                    .addKeyValue("scannedCount", result.reviewedCount())
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
