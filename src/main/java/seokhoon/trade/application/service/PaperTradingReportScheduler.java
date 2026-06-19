package seokhoon.trade.application.service;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.in.GeneratePaperTradingReportUseCase;
import seokhoon.trade.application.port.in.PaperTradingReportView;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.config.PaperTradingReportProperties;
import seokhoon.trade.domain.scheduler.*;

import java.time.*;

@Component
public class PaperTradingReportScheduler {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private final GeneratePaperTradingReportUseCase useCase;
    private final MarketCalendarPort calendar;
    private final SchedulerExecutionHistoryPort histories;
    private final OperationalMetricsPort metrics;
    private final CorrelationIdProvider correlationIds;
    private final PaperTradingReportProperties properties;
    private final Clock clock;

    @Autowired
    public PaperTradingReportScheduler(GeneratePaperTradingReportUseCase useCase, MarketCalendarPort calendar,
                                       SchedulerExecutionHistoryPort histories, OperationalMetricsPort metrics,
                                       CorrelationIdProvider correlationIds, PaperTradingReportProperties properties) {
        this(useCase, calendar, histories, metrics, correlationIds, properties, Clock.system(SEOUL));
    }
    PaperTradingReportScheduler(GeneratePaperTradingReportUseCase useCase, MarketCalendarPort calendar,
                                SchedulerExecutionHistoryPort histories, OperationalMetricsPort metrics,
                                CorrelationIdProvider correlationIds, PaperTradingReportProperties properties, Clock clock) {
        this.useCase=useCase; this.calendar=calendar; this.histories=histories; this.metrics=metrics;
        this.correlationIds=correlationIds; this.properties=properties; this.clock=clock;
    }

    @Scheduled(cron = "0 10 16 * * MON-FRI", zone = "Asia/Seoul")
    public void generateDailyReport() {
        String correlationId=correlationIds.newCorrelationId(); MDC.put("correlationId",correlationId);
        try { execute(correlationId); } finally { MDC.remove("correlationId"); }
    }
    private void execute(String correlationId) {
        LocalDate date=LocalDate.now(clock);
        if(!properties.isEnabled() || !properties.isAutoRun()) { skip(date,"DISABLED",correlationId); return; }
        if(!calendar.isTradingDay(date)) { skip(date,"NON_TRADING_DAY",correlationId); return; }
        long id=histories.saveStarted(SchedulerName.PAPER_TRADING_DAILY_REPORT,date,correlationId,clock.instant());
        metrics.recordSchedulerExecution(SchedulerName.PAPER_TRADING_DAILY_REPORT, SchedulerExecutionStatus.STARTED);
        try {
            PaperTradingReportView view=useCase.generateDailyReport(date);
            histories.markSucceeded(id,view.run().totalCandidates(),view.run().totalCandidates(),false,clock.instant());
            metrics.recordSchedulerExecution(SchedulerName.PAPER_TRADING_DAILY_REPORT, SchedulerExecutionStatus.SUCCEEDED);
            metrics.recordSchedulerSelected(SchedulerName.PAPER_TRADING_DAILY_REPORT,view.run().totalCandidates());
            metrics.recordSchedulerNotification(SchedulerName.PAPER_TRADING_DAILY_REPORT,false);
        } catch(RuntimeException exception) {
            histories.markFailed(id,failureReason(exception),clock.instant());
            metrics.recordSchedulerExecution(SchedulerName.PAPER_TRADING_DAILY_REPORT, SchedulerExecutionStatus.FAILED);
            throw exception;
        }
    }
    private void skip(LocalDate date,String reason,String correlationId) {
        histories.markSkipped(SchedulerName.PAPER_TRADING_DAILY_REPORT,date,reason,correlationId,clock.instant());
        metrics.recordSchedulerExecution(SchedulerName.PAPER_TRADING_DAILY_REPORT, SchedulerExecutionStatus.SKIPPED);
    }
    private static String failureReason(RuntimeException exception) {
        String value=exception.getClass().getSimpleName()+": "+exception.getMessage(); return value.length()<=1000?value:value.substring(0,1000);
    }
}
