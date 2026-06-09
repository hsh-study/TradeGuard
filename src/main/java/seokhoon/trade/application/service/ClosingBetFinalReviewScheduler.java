package seokhoon.trade.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import seokhoon.trade.application.port.in.ReviewClosingBetCandidatesUseCase;
import seokhoon.trade.application.port.out.MarketCalendarPort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.application.port.out.SchedulerExecutionHistoryPort;
import seokhoon.trade.domain.scheduler.SchedulerExecutionStatus;
import seokhoon.trade.domain.scheduler.SchedulerName;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Component
public class ClosingBetFinalReviewScheduler {
    private static final Logger log = LoggerFactory.getLogger(ClosingBetFinalReviewScheduler.class);
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final int DEFAULT_LIMIT = 5;

    private final ReviewClosingBetCandidatesUseCase reviewClosingBetCandidatesUseCase;
    private final MarketCalendarPort marketCalendarPort;
    private final SchedulerExecutionHistoryPort historyPort;
    private final OperationalMetricsPort metricsPort;
    private final Clock clock;

    @Autowired
    public ClosingBetFinalReviewScheduler(
            ReviewClosingBetCandidatesUseCase reviewClosingBetCandidatesUseCase,
            MarketCalendarPort marketCalendarPort,
            SchedulerExecutionHistoryPort historyPort,
            OperationalMetricsPort metricsPort
    ) {
        this(
                reviewClosingBetCandidatesUseCase,
                marketCalendarPort,
                historyPort,
                metricsPort,
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
        this.reviewClosingBetCandidatesUseCase = reviewClosingBetCandidatesUseCase;
        this.marketCalendarPort = marketCalendarPort;
        this.historyPort = historyPort;
        this.metricsPort = metricsPort;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 15 * * MON-FRI", zone = "Asia/Seoul")
    public void reviewAtMarketLateAfternoon() {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        try {
            executeScheduledReview();
        } finally {
            MDC.remove("correlationId");
        }
    }

    private void executeScheduledReview() {
        SchedulerName schedulerName = SchedulerName.CLOSING_BET_FINAL_REVIEW_15;
        LocalDate tradeDate = LocalDate.now(clock);
        if (!marketCalendarPort.isTradingDay(tradeDate)) {
            historyPort.markSkipped(
                    schedulerName,
                    tradeDate,
                    "NON_TRADING_DAY",
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
                    .log("Scheduler execution skipped");
            return;
        }
        long historyId = historyPort.saveStarted(
                schedulerName,
                tradeDate,
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
