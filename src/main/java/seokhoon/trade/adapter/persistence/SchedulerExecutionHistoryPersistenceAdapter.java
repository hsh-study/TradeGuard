package seokhoon.trade.adapter.persistence;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.out.SchedulerExecutionHistoryPort;
import seokhoon.trade.application.port.out.SchedulerExecutionHistoryRecord;
import seokhoon.trade.domain.scheduler.SchedulerExecutionHistory;
import seokhoon.trade.domain.scheduler.SchedulerExecutionStatus;
import seokhoon.trade.domain.scheduler.SchedulerName;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Component
public class SchedulerExecutionHistoryPersistenceAdapter
        implements SchedulerExecutionHistoryPort {
    private final SchedulerExecutionHistoryJpaRepository repository;

    public SchedulerExecutionHistoryPersistenceAdapter(
            SchedulerExecutionHistoryJpaRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long saveStarted(
            SchedulerName schedulerName,
            LocalDate tradeDate,
            Instant startedAt
    ) {
        return repository.saveAndFlush(SchedulerExecutionHistoryEntity.from(
                SchedulerExecutionHistory.started(schedulerName, tradeDate, startedAt)
        )).id();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSucceeded(
            long historyId,
            int scannedCount,
            int selectedCount,
            boolean notificationSent,
            Instant finishedAt
    ) {
        SchedulerExecutionHistoryEntity entity = findEntity(historyId);
        SchedulerExecutionHistory history = entity.toDomain();
        history.markSucceeded(scannedCount, selectedCount, notificationSent, finishedAt);
        entity.update(history);
        repository.saveAndFlush(entity);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSkipped(
            SchedulerName schedulerName,
            LocalDate tradeDate,
            String skipReason,
            Instant occurredAt
    ) {
        repository.saveAndFlush(SchedulerExecutionHistoryEntity.from(
                SchedulerExecutionHistory.skipped(
                        schedulerName,
                        tradeDate,
                        skipReason,
                        occurredAt
                )
        ));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(long historyId, String failureReason, Instant finishedAt) {
        SchedulerExecutionHistoryEntity entity = findEntity(historyId);
        SchedulerExecutionHistory history = entity.toDomain();
        history.markFailed(failureReason, finishedAt);
        entity.update(history);
        repository.saveAndFlush(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SchedulerExecutionHistoryRecord> find(
            LocalDate tradeDate,
            SchedulerName schedulerName,
            SchedulerExecutionStatus status
    ) {
        Specification<SchedulerExecutionHistoryEntity> specification =
                (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
        if (tradeDate != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("tradeDate"), tradeDate));
        }
        if (schedulerName != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("schedulerName"), schedulerName));
        }
        if (status != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("status"), status));
        }
        return repository.findAll(
                        specification,
                        Sort.by(Sort.Order.desc("startedAt"), Sort.Order.desc("id"))
                )
                .stream()
                .map(SchedulerExecutionHistoryEntity::toRecord)
                .toList();
    }

    private SchedulerExecutionHistoryEntity findEntity(long historyId) {
        return repository.findById(historyId)
                .orElseThrow(() -> new IllegalStateException(
                        "Scheduler execution history not found: " + historyId
                ));
    }
}
