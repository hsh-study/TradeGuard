package seokhoon.trade.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.in.LoadSchedulerExecutionHistoriesUseCase;
import seokhoon.trade.application.port.in.SchedulerExecutionHistoryView;
import seokhoon.trade.application.port.out.SchedulerExecutionHistoryPort;
import seokhoon.trade.application.port.out.SchedulerExecutionHistoryRecord;
import seokhoon.trade.domain.scheduler.SchedulerExecutionStatus;
import seokhoon.trade.domain.scheduler.SchedulerName;

import java.time.LocalDate;
import java.util.List;

@Service
public class SchedulerExecutionHistoryQueryService
        implements LoadSchedulerExecutionHistoriesUseCase {
    private final SchedulerExecutionHistoryPort historyPort;

    public SchedulerExecutionHistoryQueryService(
            SchedulerExecutionHistoryPort historyPort
    ) {
        this.historyPort = historyPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SchedulerExecutionHistoryView> load(
            LocalDate tradeDate,
            SchedulerName schedulerName,
            SchedulerExecutionStatus status
    ) {
        return historyPort.find(tradeDate, schedulerName, status).stream()
                .map(SchedulerExecutionHistoryQueryService::toView)
                .toList();
    }

    private static SchedulerExecutionHistoryView toView(
            SchedulerExecutionHistoryRecord record
    ) {
        return new SchedulerExecutionHistoryView(
                record.id(),
                record.schedulerName(),
                record.tradeDate(),
                record.status(),
                record.skipReason(),
                record.failureReason(),
                record.scannedCount(),
                record.selectedCount(),
                record.notificationSent(),
                record.startedAt(),
                record.finishedAt()
        );
    }
}
