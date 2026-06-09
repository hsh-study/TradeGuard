package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.scheduler.SchedulerExecutionStatus;
import seokhoon.trade.domain.scheduler.SchedulerName;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface SchedulerExecutionHistoryPort {
    long saveStarted(SchedulerName schedulerName, LocalDate tradeDate, Instant startedAt);

    void markSucceeded(
            long historyId,
            int scannedCount,
            int selectedCount,
            boolean notificationSent,
            Instant finishedAt
    );

    void markSkipped(
            SchedulerName schedulerName,
            LocalDate tradeDate,
            String skipReason,
            Instant occurredAt
    );

    void markFailed(long historyId, String failureReason, Instant finishedAt);

    List<SchedulerExecutionHistoryRecord> find(
            LocalDate tradeDate,
            SchedulerName schedulerName,
            SchedulerExecutionStatus status
    );

    static SchedulerExecutionHistoryPort noop() {
        return new SchedulerExecutionHistoryPort() {
            @Override
            public long saveStarted(
                    SchedulerName schedulerName,
                    LocalDate tradeDate,
                    Instant startedAt
            ) {
                return 0L;
            }

            @Override
            public void markSucceeded(
                    long historyId,
                    int scannedCount,
                    int selectedCount,
                    boolean notificationSent,
                    Instant finishedAt
            ) {
            }

            @Override
            public void markSkipped(
                    SchedulerName schedulerName,
                    LocalDate tradeDate,
                    String skipReason,
                    Instant occurredAt
            ) {
            }

            @Override
            public void markFailed(
                    long historyId,
                    String failureReason,
                    Instant finishedAt
            ) {
            }

            @Override
            public List<SchedulerExecutionHistoryRecord> find(
                    LocalDate tradeDate,
                    SchedulerName schedulerName,
                    SchedulerExecutionStatus status
            ) {
                return List.of();
            }
        };
    }
}
