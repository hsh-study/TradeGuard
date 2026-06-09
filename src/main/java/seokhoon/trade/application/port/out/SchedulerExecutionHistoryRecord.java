package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.scheduler.SchedulerExecutionStatus;
import seokhoon.trade.domain.scheduler.SchedulerName;

import java.time.Instant;
import java.time.LocalDate;

public record SchedulerExecutionHistoryRecord(
        Long id,
        SchedulerName schedulerName,
        LocalDate tradeDate,
        SchedulerExecutionStatus status,
        String skipReason,
        String failureReason,
        Integer scannedCount,
        Integer selectedCount,
        Boolean notificationSent,
        Instant startedAt,
        Instant finishedAt
) {
}
