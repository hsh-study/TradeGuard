package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.scheduler.SchedulerExecutionStatus;
import seokhoon.trade.domain.scheduler.SchedulerName;

import java.time.Instant;
import java.time.LocalDate;

public record SchedulerExecutionHistoryView(
        Long id,
        SchedulerName schedulerName,
        LocalDate tradeDate,
        SchedulerExecutionStatus status,
        String skipReason,
        String failureReason,
        Integer scannedCount,
        Integer selectedCount,
        Boolean notificationSent,
        String correlationId,
        Instant startedAt,
        Instant finishedAt
) {
}
