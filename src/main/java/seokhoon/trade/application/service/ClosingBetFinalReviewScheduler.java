package seokhoon.trade.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import seokhoon.trade.application.port.in.ReviewClosingBetCandidatesUseCase;
import seokhoon.trade.application.port.out.MarketCalendarPort;
import seokhoon.trade.application.port.out.SchedulerExecutionHistoryPort;
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
    private final Clock clock;

    @Autowired
    public ClosingBetFinalReviewScheduler(
            ReviewClosingBetCandidatesUseCase reviewClosingBetCandidatesUseCase,
            MarketCalendarPort marketCalendarPort,
            SchedulerExecutionHistoryPort historyPort
    ) {
        this(
                reviewClosingBetCandidatesUseCase,
                marketCalendarPort,
                historyPort,
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
                clock
        );
    }

    ClosingBetFinalReviewScheduler(
            ReviewClosingBetCandidatesUseCase reviewClosingBetCandidatesUseCase,
            MarketCalendarPort marketCalendarPort,
            SchedulerExecutionHistoryPort historyPort,
            Clock clock
    ) {
        this.reviewClosingBetCandidatesUseCase = reviewClosingBetCandidatesUseCase;
        this.marketCalendarPort = marketCalendarPort;
        this.historyPort = historyPort;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 15 * * MON-FRI", zone = "Asia/Seoul")
    public void reviewAtMarketLateAfternoon() {
        LocalDate tradeDate = LocalDate.now(clock);
        if (!marketCalendarPort.isTradingDay(tradeDate)) {
            historyPort.markSkipped(
                    SchedulerName.CLOSING_BET_FINAL_REVIEW_15,
                    tradeDate,
                    "NON_TRADING_DAY",
                    Instant.now(clock)
            );
            log.info("Skipping 15:00 closing bet final review on non-trading day: {}", tradeDate);
            return;
        }
        long historyId = historyPort.saveStarted(
                SchedulerName.CLOSING_BET_FINAL_REVIEW_15,
                tradeDate,
                Instant.now(clock)
        );
        try {
            var result = reviewClosingBetCandidatesUseCase.review(tradeDate, DEFAULT_LIMIT);
            historyPort.markSucceeded(
                    historyId,
                    result.reviewedCount(),
                    result.selectedCount(),
                    result.briefingSent(),
                    Instant.now(clock)
            );
        } catch (RuntimeException exception) {
            historyPort.markFailed(
                    historyId,
                    failureReason(exception),
                    Instant.now(clock)
            );
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
