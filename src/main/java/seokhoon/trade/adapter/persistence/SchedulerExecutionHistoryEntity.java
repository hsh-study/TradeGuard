package seokhoon.trade.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import seokhoon.trade.application.port.out.SchedulerExecutionHistoryRecord;
import seokhoon.trade.domain.scheduler.SchedulerExecutionHistory;
import seokhoon.trade.domain.scheduler.SchedulerExecutionStatus;
import seokhoon.trade.domain.scheduler.SchedulerName;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "scheduler_execution_histories")
public class SchedulerExecutionHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(name = "scheduler_name", nullable = false)
    private SchedulerName schedulerName;
    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SchedulerExecutionStatus status;
    @Column(name = "skip_reason", length = 1000)
    private String skipReason;
    @Column(name = "failure_reason", length = 1000)
    private String failureReason;
    @Column(name = "scanned_count")
    private Integer scannedCount;
    @Column(name = "selected_count")
    private Integer selectedCount;
    @Column(name = "notification_sent")
    private Boolean notificationSent;
    @Column(name = "correlation_id", length = 128)
    private String correlationId;
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;
    @Column(name = "finished_at")
    private Instant finishedAt;

    protected SchedulerExecutionHistoryEntity() {
    }

    static SchedulerExecutionHistoryEntity from(SchedulerExecutionHistory history) {
        SchedulerExecutionHistoryEntity entity = new SchedulerExecutionHistoryEntity();
        entity.update(history);
        return entity;
    }

    void update(SchedulerExecutionHistory history) {
        schedulerName = history.schedulerName();
        tradeDate = history.tradeDate();
        status = history.status();
        skipReason = history.skipReason();
        failureReason = history.failureReason();
        scannedCount = history.scannedCount();
        selectedCount = history.selectedCount();
        notificationSent = history.notificationSent();
        correlationId = history.correlationId();
        startedAt = history.startedAt();
        finishedAt = history.finishedAt();
    }

    Long id() {
        return id;
    }

    SchedulerExecutionHistory toDomain() {
        return SchedulerExecutionHistory.restore(
                schedulerName,
                tradeDate,
                status,
                skipReason,
                failureReason,
                scannedCount,
                selectedCount,
                notificationSent,
                correlationId,
                startedAt,
                finishedAt
        );
    }

    SchedulerExecutionHistoryRecord toRecord() {
        return new SchedulerExecutionHistoryRecord(
                id,
                schedulerName,
                tradeDate,
                status,
                skipReason,
                failureReason,
                scannedCount,
                selectedCount,
                notificationSent,
                correlationId,
                startedAt,
                finishedAt
        );
    }
}
