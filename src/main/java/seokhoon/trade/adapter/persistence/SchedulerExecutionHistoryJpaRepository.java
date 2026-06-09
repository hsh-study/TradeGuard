package seokhoon.trade.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SchedulerExecutionHistoryJpaRepository
        extends JpaRepository<SchedulerExecutionHistoryEntity, Long>,
        JpaSpecificationExecutor<SchedulerExecutionHistoryEntity> {
}
