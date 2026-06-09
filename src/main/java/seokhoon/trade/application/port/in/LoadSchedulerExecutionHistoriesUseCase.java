package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.scheduler.SchedulerExecutionStatus;
import seokhoon.trade.domain.scheduler.SchedulerName;

import java.time.LocalDate;
import java.util.List;

public interface LoadSchedulerExecutionHistoriesUseCase {
    List<SchedulerExecutionHistoryView> load(
            LocalDate tradeDate,
            SchedulerName schedulerName,
            SchedulerExecutionStatus status
    );
}
