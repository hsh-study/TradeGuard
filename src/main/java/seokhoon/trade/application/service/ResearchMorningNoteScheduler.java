package seokhoon.trade.application.service;

import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import seokhoon.trade.application.port.in.ResearchUseCases.MorningNoteUseCase;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.config.ResearchProperties;
import seokhoon.trade.domain.research.MorningNote;
import seokhoon.trade.domain.scheduler.SchedulerExecutionStatus;
import seokhoon.trade.domain.scheduler.SchedulerName;

import java.time.*;

@Component
public class ResearchMorningNoteScheduler {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final MorningNoteUseCase useCase;
    private final MarketCalendarPort calendarPort;
    private final SchedulerExecutionHistoryPort historyPort;
    private final OperationalMetricsPort metrics;
    private final CorrelationIdProvider correlationIds;
    private final NotificationPort notificationPort;
    private final ResearchProperties properties;
    private final Clock clock;

    @Autowired
    public ResearchMorningNoteScheduler(
            MorningNoteUseCase useCase,
            MarketCalendarPort calendarPort,
            SchedulerExecutionHistoryPort historyPort,
            OperationalMetricsPort metrics,
            CorrelationIdProvider correlationIds,
            NotificationPort notificationPort,
            ResearchProperties properties
    ) {
        this(useCase, calendarPort, historyPort, metrics, correlationIds,
                notificationPort, properties, Clock.system(SEOUL));
    }

    ResearchMorningNoteScheduler(
            MorningNoteUseCase useCase,
            MarketCalendarPort calendarPort,
            SchedulerExecutionHistoryPort historyPort,
            OperationalMetricsPort metrics,
            CorrelationIdProvider correlationIds,
            NotificationPort notificationPort,
            ResearchProperties properties,
            Clock clock
    ) {
        this.useCase = useCase;
        this.calendarPort = calendarPort;
        this.historyPort = historyPort;
        this.metrics = metrics;
        this.correlationIds = correlationIds;
        this.notificationPort = notificationPort;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(cron = "0 10 8 * * MON-FRI", zone = "Asia/Seoul")
    public void generateMorningNote() {
        String correlationId = correlationIds.newCorrelationId();
        MDC.put("correlationId", correlationId);
        try {
            execute(correlationId);
        } finally {
            MDC.remove("correlationId");
        }
    }

    private void execute(String correlationId) {
        LocalDate tradeDate = LocalDate.now(clock);
        if (!calendarPort.isTradingDay(tradeDate)) {
            historyPort.markSkipped(SchedulerName.RESEARCH_MORNING_NOTE, tradeDate,
                    "NON_TRADING_DAY", correlationId, clock.instant());
            metrics.recordSchedulerExecution(
                    SchedulerName.RESEARCH_MORNING_NOTE, SchedulerExecutionStatus.SKIPPED);
            return;
        }
        long historyId = historyPort.saveStarted(
                SchedulerName.RESEARCH_MORNING_NOTE, tradeDate, correlationId, clock.instant());
        metrics.recordSchedulerExecution(
                SchedulerName.RESEARCH_MORNING_NOTE, SchedulerExecutionStatus.STARTED);
        try {
            MorningNote note = useCase.generate(tradeDate);
            boolean notificationSent = sendOptional(note);
            historyPort.markSucceeded(historyId, 1, 1, notificationSent, clock.instant());
            metrics.recordSchedulerExecution(
                    SchedulerName.RESEARCH_MORNING_NOTE, SchedulerExecutionStatus.SUCCEEDED);
            metrics.recordSchedulerNotification(SchedulerName.RESEARCH_MORNING_NOTE, notificationSent);
        } catch (RuntimeException exception) {
            historyPort.markFailed(historyId, failureReason(exception), clock.instant());
            metrics.recordSchedulerExecution(
                    SchedulerName.RESEARCH_MORNING_NOTE, SchedulerExecutionStatus.FAILED);
            throw exception;
        }
    }

    private boolean sendOptional(MorningNote note) {
        if (!properties.isMorningNoteDiscordEnabled()) {
            return false;
        }
        String body = note.marketSummary() + "\n\n" + note.sectorSummary()
                + "\n\n" + note.portfolioImpactSummary()
                + "\n\n" + note.watchlistSummary()
                + "\n\n" + note.actionItems();
        return notificationPort.send(new NotificationMessage(
                "TradeGuard Morning Note - " + note.tradeDate(), body, clock.instant())).sent();
    }

    private static String failureReason(RuntimeException exception) {
        String message = exception.getMessage();
        String reason = exception.getClass().getSimpleName()
                + (message == null || message.isBlank() ? "" : ": " + message);
        return reason.length() <= 1000 ? reason : reason.substring(0, 1000);
    }
}
