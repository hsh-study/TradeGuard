package seokhoon.trade.application.service;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.in.ImportMarketIndexUseCase;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.config.MarketIndexProviderProperties;
import seokhoon.trade.domain.market.MarketIndexImportHistory;
import seokhoon.trade.domain.market.MarketIndexImportStatus;
import seokhoon.trade.domain.scheduler.SchedulerExecutionStatus;
import seokhoon.trade.domain.scheduler.SchedulerName;

import java.time.*;

@Component
public class MarketIndexImportScheduler {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final ImportMarketIndexUseCase useCase;
    private final MarketCalendarPort calendarPort;
    private final SchedulerExecutionHistoryPort historyPort;
    private final OperationalMetricsPort metrics;
    private final CorrelationIdProvider correlationIds;
    private final MarketIndexProviderProperties properties;
    private final Clock clock;

    @Autowired
    public MarketIndexImportScheduler(
            ImportMarketIndexUseCase useCase,
            MarketCalendarPort calendarPort,
            SchedulerExecutionHistoryPort historyPort,
            OperationalMetricsPort metrics,
            CorrelationIdProvider correlationIds,
            MarketIndexProviderProperties properties
    ) {
        this(useCase, calendarPort, historyPort, metrics, correlationIds,
                properties, Clock.system(SEOUL));
    }

    MarketIndexImportScheduler(
            ImportMarketIndexUseCase useCase,
            MarketCalendarPort calendarPort,
            SchedulerExecutionHistoryPort historyPort,
            OperationalMetricsPort metrics,
            CorrelationIdProvider correlationIds,
            MarketIndexProviderProperties properties,
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

    @Scheduled(cron = "0 50 7 * * MON-FRI", zone = "Asia/Seoul")
    public void importMarketIndices() {
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
        if (!properties.isImportAutoRun()) {
            historyPort.markSkipped(SchedulerName.MARKET_INDEX_IMPORT, today,
                    "DISABLED", correlationId, clock.instant());
            metrics.recordSchedulerExecution(
                    SchedulerName.MARKET_INDEX_IMPORT, SchedulerExecutionStatus.SKIPPED);
            return;
        }
        if (!properties.isEnabled()) {
            historyPort.markSkipped(SchedulerName.MARKET_INDEX_IMPORT, today,
                    "PROVIDER_DISABLED", correlationId, clock.instant());
            metrics.recordSchedulerExecution(
                    SchedulerName.MARKET_INDEX_IMPORT, SchedulerExecutionStatus.SKIPPED);
            return;
        }
        if (!calendarPort.isTradingDay(today)) {
            historyPort.markSkipped(SchedulerName.MARKET_INDEX_IMPORT, today,
                    "NON_TRADING_DAY", correlationId, clock.instant());
            metrics.recordSchedulerExecution(
                    SchedulerName.MARKET_INDEX_IMPORT, SchedulerExecutionStatus.SKIPPED);
            return;
        }
        LocalDate tradeDate = calendarPort.previousTradingDay(today);
        long historyId = historyPort.saveStarted(
                SchedulerName.MARKET_INDEX_IMPORT, tradeDate, correlationId, clock.instant());
        metrics.recordSchedulerExecution(
                SchedulerName.MARKET_INDEX_IMPORT, SchedulerExecutionStatus.STARTED);
        try {
            MarketIndexImportHistory result = useCase.importProvider(tradeDate);
            if (result.status() == MarketIndexImportStatus.FAILED) {
                historyPort.markFailed(historyId, result.failureReason(), clock.instant());
                metrics.recordSchedulerExecution(
                        SchedulerName.MARKET_INDEX_IMPORT, SchedulerExecutionStatus.FAILED);
                return;
            }
            historyPort.markSucceeded(historyId, 3, result.importedCount(), false, clock.instant());
            metrics.recordSchedulerExecution(
                    SchedulerName.MARKET_INDEX_IMPORT,
                    result.status() == MarketIndexImportStatus.SKIPPED
                            ? SchedulerExecutionStatus.SKIPPED
                            : SchedulerExecutionStatus.SUCCEEDED);
            metrics.recordSchedulerSelected(SchedulerName.MARKET_INDEX_IMPORT, result.importedCount());
            metrics.recordSchedulerNotification(SchedulerName.MARKET_INDEX_IMPORT, false);
        } catch (RuntimeException exception) {
            historyPort.markFailed(historyId, failureReason(exception), clock.instant());
            metrics.recordSchedulerExecution(
                    SchedulerName.MARKET_INDEX_IMPORT, SchedulerExecutionStatus.FAILED);
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
