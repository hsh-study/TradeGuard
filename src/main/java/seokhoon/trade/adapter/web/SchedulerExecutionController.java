package seokhoon.trade.adapter.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import seokhoon.trade.application.port.in.LoadSchedulerExecutionHistoriesUseCase;
import seokhoon.trade.application.port.in.SchedulerExecutionHistoryView;
import seokhoon.trade.domain.scheduler.SchedulerExecutionStatus;
import seokhoon.trade.domain.scheduler.SchedulerName;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/scheduler-executions")
public class SchedulerExecutionController {
    private final LoadSchedulerExecutionHistoriesUseCase loadHistories;

    public SchedulerExecutionController(
            LoadSchedulerExecutionHistoriesUseCase loadHistories
    ) {
        this.loadHistories = loadHistories;
    }

    @GetMapping
    List<SchedulerExecutionResponse> find(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tradeDate,
            @RequestParam(required = false) SchedulerName schedulerName,
            @RequestParam(required = false) SchedulerExecutionStatus status
    ) {
        return loadHistories.load(tradeDate, schedulerName, status).stream()
                .map(SchedulerExecutionResponse::from)
                .toList();
    }

    public record SchedulerExecutionResponse(
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
        static SchedulerExecutionResponse from(SchedulerExecutionHistoryView view) {
            return new SchedulerExecutionResponse(
                    view.id(),
                    view.schedulerName(),
                    view.tradeDate(),
                    view.status(),
                    view.skipReason(),
                    view.failureReason(),
                    view.scannedCount(),
                    view.selectedCount(),
                    view.notificationSent(),
                    view.correlationId(),
                    view.startedAt(),
                    view.finishedAt()
            );
        }
    }
}
