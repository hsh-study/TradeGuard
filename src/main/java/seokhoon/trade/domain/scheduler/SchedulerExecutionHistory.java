package seokhoon.trade.domain.scheduler;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public class SchedulerExecutionHistory {
    private final SchedulerName schedulerName;
    private final LocalDate tradeDate;
    private SchedulerExecutionStatus status;
    private String skipReason;
    private String failureReason;
    private Integer scannedCount;
    private Integer selectedCount;
    private Boolean notificationSent;
    private final String correlationId;
    private final Instant startedAt;
    private Instant finishedAt;

    private SchedulerExecutionHistory(
            SchedulerName schedulerName,
            LocalDate tradeDate,
            SchedulerExecutionStatus status,
            String correlationId,
            Instant startedAt
    ) {
        this.schedulerName = Objects.requireNonNull(schedulerName, "schedulerName");
        this.tradeDate = Objects.requireNonNull(tradeDate, "tradeDate");
        this.status = Objects.requireNonNull(status, "status");
        this.correlationId = correlationId;
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
    }

    public static SchedulerExecutionHistory started(
            SchedulerName schedulerName,
            LocalDate tradeDate,
            String correlationId,
            Instant startedAt
    ) {
        requireReason(correlationId, "correlationId");
        return new SchedulerExecutionHistory(
                schedulerName,
                tradeDate,
                SchedulerExecutionStatus.STARTED,
                correlationId,
                startedAt
        );
    }

    public static SchedulerExecutionHistory skipped(
            SchedulerName schedulerName,
            LocalDate tradeDate,
            String reason,
            String correlationId,
            Instant occurredAt
    ) {
        requireReason(correlationId, "correlationId");
        SchedulerExecutionHistory history = new SchedulerExecutionHistory(
                schedulerName,
                tradeDate,
                SchedulerExecutionStatus.SKIPPED,
                correlationId,
                occurredAt
        );
        history.skipReason = requireReason(reason, "skip reason");
        history.finishedAt = occurredAt;
        return history;
    }

    public static SchedulerExecutionHistory restore(
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
        SchedulerExecutionHistory history = new SchedulerExecutionHistory(
                schedulerName,
                tradeDate,
                status,
                correlationId,
                startedAt
        );
        history.skipReason = skipReason;
        history.failureReason = failureReason;
        history.scannedCount = scannedCount;
        history.selectedCount = selectedCount;
        history.notificationSent = notificationSent;
        history.finishedAt = finishedAt;
        return history;
    }

    public void markSucceeded(
            int scannedCount,
            int selectedCount,
            boolean notificationSent,
            Instant finishedAt
    ) {
        requireStarted();
        if (scannedCount < 0 || selectedCount < 0) {
            throw new IllegalArgumentException("scheduler result counts must not be negative");
        }
        this.status = SchedulerExecutionStatus.SUCCEEDED;
        this.scannedCount = scannedCount;
        this.selectedCount = selectedCount;
        this.notificationSent = notificationSent;
        this.finishedAt = Objects.requireNonNull(finishedAt, "finishedAt");
    }

    public void markFailed(String reason, Instant finishedAt) {
        requireStarted();
        this.status = SchedulerExecutionStatus.FAILED;
        this.failureReason = requireReason(reason, "failure reason");
        this.finishedAt = Objects.requireNonNull(finishedAt, "finishedAt");
    }

    private void requireStarted() {
        if (status != SchedulerExecutionStatus.STARTED) {
            throw new IllegalStateException("only STARTED scheduler executions can finish");
        }
    }

    private static String requireReason(String reason, String label) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return reason;
    }

    public SchedulerName schedulerName() { return schedulerName; }
    public LocalDate tradeDate() { return tradeDate; }
    public SchedulerExecutionStatus status() { return status; }
    public String skipReason() { return skipReason; }
    public String failureReason() { return failureReason; }
    public Integer scannedCount() { return scannedCount; }
    public Integer selectedCount() { return selectedCount; }
    public Boolean notificationSent() { return notificationSent; }
    public String correlationId() { return correlationId; }
    public Instant startedAt() { return startedAt; }
    public Instant finishedAt() { return finishedAt; }
}
