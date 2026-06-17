package seokhoon.trade.application.service;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.in.GenerateValuationSnapshotUseCase;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.config.ResearchProperties;
import seokhoon.trade.domain.research.ValuationGenerationResult;
import seokhoon.trade.domain.research.ValuationGenerationStatus;
import seokhoon.trade.domain.scheduler.SchedulerExecutionStatus;
import seokhoon.trade.domain.scheduler.SchedulerName;

import java.time.*;
import java.util.List;

@Component
public class ValuationAutoSnapshotScheduler {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final GenerateValuationSnapshotUseCase useCase;
    private final MarketCalendarPort calendarPort;
    private final SchedulerExecutionHistoryPort historyPort;
    private final OperationalMetricsPort metrics;
    private final CorrelationIdProvider correlationIds;
    private final ResearchProperties properties;
    private final Clock clock;

    @Autowired
    public ValuationAutoSnapshotScheduler(
            GenerateValuationSnapshotUseCase useCase,
            MarketCalendarPort calendarPort,
            SchedulerExecutionHistoryPort historyPort,
            OperationalMetricsPort metrics,
            CorrelationIdProvider correlationIds,
            ResearchProperties properties
    ) {
        this(useCase, calendarPort, historyPort, metrics, correlationIds,
                properties, Clock.system(SEOUL));
    }

    ValuationAutoSnapshotScheduler(
            GenerateValuationSnapshotUseCase useCase,
            MarketCalendarPort calendarPort,
            SchedulerExecutionHistoryPort historyPort,
            OperationalMetricsPort metrics,
            CorrelationIdProvider correlationIds,
            ResearchProperties properties,
            Clock clock
    ) {
        this.useCase = useCase;
        this.calendarPort = calendarPort;
        this.historyPort = historyPort;
        this.metrics = metrics;
        this.correlationIds = correlationIds;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(cron = "0 55 7 * * MON-FRI", zone = "Asia/Seoul")
    public void generateWatchlistValuationSnapshots() {
        String correlationId = correlationIds.newCorrelationId();
        MDC.put("correlationId", correlationId);
        try {
            execute(correlationId);
        } finally {
            MDC.remove("correlationId");
        }
    }

    private void execute(String correlationId) {
        LocalDate today = LocalDate.now(clock);
        if (!properties.isValuationAutoSnapshotEnabled()) {
            historyPort.markSkipped(SchedulerName.VALUATION_AUTO_SNAPSHOT, today,
                    "DISABLED", correlationId, clock.instant());
            metrics.recordSchedulerExecution(
                    SchedulerName.VALUATION_AUTO_SNAPSHOT, SchedulerExecutionStatus.SKIPPED);
            return;
        }
        if (!calendarPort.isTradingDay(today)) {
            historyPort.markSkipped(SchedulerName.VALUATION_AUTO_SNAPSHOT, today,
                    "NON_TRADING_DAY", correlationId, clock.instant());
            metrics.recordSchedulerExecution(
                    SchedulerName.VALUATION_AUTO_SNAPSHOT, SchedulerExecutionStatus.SKIPPED);
            return;
        }
        LocalDate baseDate = calendarPort.previousTradingDay(today);
        long historyId = historyPort.saveStarted(
                SchedulerName.VALUATION_AUTO_SNAPSHOT, baseDate, correlationId, clock.instant());
        metrics.recordSchedulerExecution(
                SchedulerName.VALUATION_AUTO_SNAPSHOT, SchedulerExecutionStatus.STARTED);
        try {
            List<ValuationGenerationResult> results = useCase.generateWatchlist(baseDate);
            int generated = (int) results.stream()
                    .filter(result -> result.status() == ValuationGenerationStatus.GENERATED)
                    .count();
            historyPort.markSucceeded(historyId, results.size(), generated, false, clock.instant());
            metrics.recordSchedulerExecution(
                    SchedulerName.VALUATION_AUTO_SNAPSHOT, SchedulerExecutionStatus.SUCCEEDED);
            metrics.recordSchedulerSelected(SchedulerName.VALUATION_AUTO_SNAPSHOT, generated);
            metrics.recordSchedulerNotification(SchedulerName.VALUATION_AUTO_SNAPSHOT, false);
        } catch (RuntimeException exception) {
            historyPort.markFailed(historyId, failureReason(exception), clock.instant());
            metrics.recordSchedulerExecution(
                    SchedulerName.VALUATION_AUTO_SNAPSHOT, SchedulerExecutionStatus.FAILED);
            throw exception;
        }
    }

    private static String failureReason(RuntimeException exception) {
        String message = exception.getMessage();
        String reason = exception.getClass().getSimpleName()
                + (message == null || message.isBlank() ? "" : ": " + message);
        return reason.length() <= 1000 ? reason : reason.substring(0, 1000);
    }
}
