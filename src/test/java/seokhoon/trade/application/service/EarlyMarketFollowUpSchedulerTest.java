package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.EarlyMarketFollowUpResult;
import seokhoon.trade.application.port.out.CorrelationIdProvider;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.application.port.out.SchedulerExecutionHistoryPort;
import seokhoon.trade.application.port.out.SchedulerExecutionHistoryRecord;
import seokhoon.trade.domain.scheduler.SchedulerExecutionStatus;
import seokhoon.trade.domain.scheduler.SchedulerName;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EarlyMarketFollowUpSchedulerTest {
    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 6, 10);
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-10T00:20:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @Test
    void recordsSucceededHistoryOnTradingDay() {
        RecordingHistory history = new RecordingHistory();
        EarlyMarketFollowUpScheduler scheduler = scheduler(
                tradeDate -> result(false),
                date -> true,
                history
        );

        scheduler.followUpAfterOpening();

        assertThat(history.startedName).isEqualTo(SchedulerName.EARLY_MARKET_FOLLOW_UP_920);
        assertThat(history.scannedCount).isEqualTo(4);
        assertThat(history.selectedCount).isEqualTo(2);
        assertThat(history.notificationSent).isFalse();
    }

    @Test
    void recordsSkippedHistoryOnNonTradingDay() {
        RecordingHistory history = new RecordingHistory();
        EarlyMarketFollowUpScheduler scheduler = scheduler(
                tradeDate -> {
                    throw new AssertionError("follow-up must not run");
                },
                date -> false,
                history
        );

        scheduler.followUpAfterOpening();

        assertThat(history.skippedName).isEqualTo(SchedulerName.EARLY_MARKET_FOLLOW_UP_920);
        assertThat(history.skipReason).isEqualTo("NON_TRADING_DAY");
    }

    @Test
    void recordsFailedHistoryWhenUseCaseThrows() {
        RecordingHistory history = new RecordingHistory();
        EarlyMarketFollowUpScheduler scheduler = scheduler(
                tradeDate -> {
                    throw new IllegalStateException("follow-up failed");
                },
                date -> true,
                history
        );

        assertThatThrownBy(scheduler::followUpAfterOpening)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("follow-up failed");
        assertThat(history.failedId).isEqualTo(1L);
        assertThat(history.failureReason).contains("follow-up failed");
    }

    @Test
    void treatsDiscordNoOpResultAsSuccessfulExecution() {
        RecordingHistory history = new RecordingHistory();
        EarlyMarketFollowUpScheduler scheduler = scheduler(
                tradeDate -> result(false),
                date -> true,
                history
        );

        scheduler.followUpAfterOpening();

        assertThat(history.scannedCount).isEqualTo(4);
        assertThat(history.notificationSent).isFalse();
        assertThat(history.failedId).isZero();
    }

    private static EarlyMarketFollowUpScheduler scheduler(
            seokhoon.trade.application.port.in.FollowUpEarlyMarketCandidatesUseCase useCase,
            seokhoon.trade.application.port.out.MarketCalendarPort calendar,
            RecordingHistory history
    ) {
        return new EarlyMarketFollowUpScheduler(
                useCase,
                calendar,
                history,
                OperationalMetricsPort.noop(),
                new CorrelationIdProvider() {
                    @Override
                    public String currentCorrelationId() {
                        return "follow-up-correlation";
                    }

                    @Override
                    public String newCorrelationId() {
                        return "follow-up-correlation";
                    }
                },
                CLOCK
        );
    }

    private static EarlyMarketFollowUpResult result(boolean sent) {
        return new EarlyMarketFollowUpResult(
                TRADE_DATE,
                4,
                2,
                1,
                1,
                sent,
                List.of()
        );
    }

    private static class RecordingHistory implements SchedulerExecutionHistoryPort {
        private SchedulerName startedName;
        private SchedulerName skippedName;
        private String skipReason;
        private int scannedCount;
        private int selectedCount;
        private boolean notificationSent;
        private long failedId;
        private String failureReason;

        @Override
        public long saveStarted(
                SchedulerName schedulerName,
                LocalDate tradeDate,
                String correlationId,
                Instant startedAt
        ) {
            this.startedName = schedulerName;
            return 1L;
        }

        @Override
        public void markSucceeded(
                long historyId,
                int scannedCount,
                int selectedCount,
                boolean notificationSent,
                Instant finishedAt
        ) {
            this.scannedCount = scannedCount;
            this.selectedCount = selectedCount;
            this.notificationSent = notificationSent;
        }

        @Override
        public void markSkipped(
                SchedulerName schedulerName,
                LocalDate tradeDate,
                String skipReason,
                String correlationId,
                Instant occurredAt
        ) {
            this.skippedName = schedulerName;
            this.skipReason = skipReason;
        }

        @Override
        public void markFailed(long historyId, String failureReason, Instant finishedAt) {
            this.failedId = historyId;
            this.failureReason = failureReason;
        }

        @Override
        public List<SchedulerExecutionHistoryRecord> find(
                LocalDate tradeDate,
                SchedulerName schedulerName,
                SchedulerExecutionStatus status
        ) {
            return List.of();
        }
    }
}
