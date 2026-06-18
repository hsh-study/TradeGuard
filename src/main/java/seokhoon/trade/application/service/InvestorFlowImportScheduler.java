package seokhoon.trade.application.service;

import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.in.ImportInvestorFlowsUseCase;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.config.InvestorFlowProperties;
import seokhoon.trade.domain.market.InvestorFlowImportStatus;
import seokhoon.trade.domain.scheduler.*;
import java.time.*;

@Component
public class InvestorFlowImportScheduler {
    private static final ZoneId SEOUL=ZoneId.of("Asia/Seoul");
    private final ImportInvestorFlowsUseCase useCase; private final MarketCalendarPort calendar;
    private final SchedulerExecutionHistoryPort histories; private final OperationalMetricsPort metrics;
    private final CorrelationIdProvider correlations; private final InvestorFlowProperties properties; private final Clock clock;
    @org.springframework.beans.factory.annotation.Autowired
    public InvestorFlowImportScheduler(ImportInvestorFlowsUseCase useCase,MarketCalendarPort calendar,
            SchedulerExecutionHistoryPort histories,OperationalMetricsPort metrics,CorrelationIdProvider correlations,
            InvestorFlowProperties properties){this(useCase,calendar,histories,metrics,correlations,properties,Clock.system(SEOUL));}
    InvestorFlowImportScheduler(ImportInvestorFlowsUseCase useCase,MarketCalendarPort calendar,
            SchedulerExecutionHistoryPort histories,OperationalMetricsPort metrics,CorrelationIdProvider correlations,
            InvestorFlowProperties properties,Clock clock){this.useCase=useCase;this.calendar=calendar;this.histories=histories;this.metrics=metrics;this.correlations=correlations;this.properties=properties;this.clock=clock;}
    @Scheduled(cron="0 40 7 * * MON-FRI",zone="Asia/Seoul") public void importFlows(){String id=correlations.newCorrelationId();MDC.put("correlationId",id);try{execute(id);}finally{MDC.remove("correlationId");}}
    void execute(String id){LocalDate today=LocalDate.now(clock);if(!calendar.isTradingDay(today)){skip(today,"NON_TRADING_DAY",id);return;}LocalDate date=calendar.previousTradingDay(today);
        if(!properties.isImportAutoRun()){skip(date,"DISABLED",id);return;}
        if(!properties.isProviderEnabled()){skip(date,"PROVIDER_DISABLED",id);return;}
        if(properties.isKisProviderWithUnverifiedAmountUnit()){skip(date,"AMOUNT_UNIT_UNVERIFIED",id);return;}
        long historyId=histories.saveStarted(SchedulerName.INVESTOR_FLOW_IMPORT,date,id,clock.instant());metrics.recordSchedulerExecution(SchedulerName.INVESTOR_FLOW_IMPORT,SchedulerExecutionStatus.STARTED);
        try{var results=useCase.importWatchlist(date);int imported=results.stream().mapToInt(r->r.importedCount()).sum();boolean failed=results.stream().anyMatch(r->r.status()==InvestorFlowImportStatus.FAILED);if(failed){histories.markFailed(historyId,"INVESTOR_FLOW_IMPORT_FAILED",clock.instant());metrics.recordSchedulerExecution(SchedulerName.INVESTOR_FLOW_IMPORT,SchedulerExecutionStatus.FAILED);}else{histories.markSucceeded(historyId,results.size(),imported,false,clock.instant());metrics.recordSchedulerExecution(SchedulerName.INVESTOR_FLOW_IMPORT,results.stream().allMatch(r->r.status()==InvestorFlowImportStatus.SKIPPED)?SchedulerExecutionStatus.SKIPPED:SchedulerExecutionStatus.SUCCEEDED);metrics.recordSchedulerSelected(SchedulerName.INVESTOR_FLOW_IMPORT,imported);}}
        catch(RuntimeException e){histories.markFailed(historyId,e.getClass().getSimpleName(),clock.instant());metrics.recordSchedulerExecution(SchedulerName.INVESTOR_FLOW_IMPORT,SchedulerExecutionStatus.FAILED);}}
    private void skip(LocalDate d,String reason,String id){histories.markSkipped(SchedulerName.INVESTOR_FLOW_IMPORT,d,reason,id,clock.instant());metrics.recordSchedulerExecution(SchedulerName.INVESTOR_FLOW_IMPORT,SchedulerExecutionStatus.SKIPPED);}
}
