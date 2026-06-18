package seokhoon.trade.application.service;

import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.in.AnalyzeSupplyDemandUseCase;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.domain.scheduler.*;
import seokhoon.trade.config.InvestorFlowProperties;
import java.time.*;

@Component
public class SupplyDemandAnalysisScheduler {
    private static final ZoneId SEOUL=ZoneId.of("Asia/Seoul");
    private final AnalyzeSupplyDemandUseCase useCase; private final MarketCalendarPort calendar;
    private final SchedulerExecutionHistoryPort histories; private final OperationalMetricsPort metrics;
    private final CorrelationIdProvider correlations; private final InvestorFlowProperties properties; private final Clock clock;
    @org.springframework.beans.factory.annotation.Autowired
    public SupplyDemandAnalysisScheduler(AnalyzeSupplyDemandUseCase useCase,MarketCalendarPort calendar,
            SchedulerExecutionHistoryPort histories,OperationalMetricsPort metrics,CorrelationIdProvider correlations,
            InvestorFlowProperties properties){this(useCase,calendar,histories,metrics,correlations,properties,Clock.system(SEOUL));}
    SupplyDemandAnalysisScheduler(AnalyzeSupplyDemandUseCase useCase,MarketCalendarPort calendar,
            SchedulerExecutionHistoryPort histories,OperationalMetricsPort metrics,CorrelationIdProvider correlations,
            InvestorFlowProperties properties,Clock clock){this.useCase=useCase;this.calendar=calendar;this.histories=histories;this.metrics=metrics;this.correlations=correlations;this.properties=properties;this.clock=clock;}
    @Scheduled(cron="0 45 7 * * MON-FRI",zone="Asia/Seoul") public void analyze(){String id=correlations.newCorrelationId();MDC.put("correlationId",id);try{execute(id);}finally{MDC.remove("correlationId");}}
    void execute(String id){LocalDate today=LocalDate.now(clock);if(!calendar.isTradingDay(today)){histories.markSkipped(SchedulerName.SUPPLY_DEMAND_ANALYSIS,today,"NON_TRADING_DAY",id,clock.instant());metrics.recordSchedulerExecution(SchedulerName.SUPPLY_DEMAND_ANALYSIS,SchedulerExecutionStatus.SKIPPED);return;}LocalDate date=calendar.previousTradingDay(today);if(properties.isKisProviderWithUnverifiedAmountUnit()){histories.markSkipped(SchedulerName.SUPPLY_DEMAND_ANALYSIS,date,"AMOUNT_UNIT_UNVERIFIED",id,clock.instant());metrics.recordSchedulerExecution(SchedulerName.SUPPLY_DEMAND_ANALYSIS,SchedulerExecutionStatus.SKIPPED);return;}long historyId=histories.saveStarted(SchedulerName.SUPPLY_DEMAND_ANALYSIS,date,id,clock.instant());metrics.recordSchedulerExecution(SchedulerName.SUPPLY_DEMAND_ANALYSIS,SchedulerExecutionStatus.STARTED);try{var result=useCase.analyzeWatchlist(date);histories.markSucceeded(historyId,result.size(),result.size(),false,clock.instant());metrics.recordSchedulerExecution(SchedulerName.SUPPLY_DEMAND_ANALYSIS,SchedulerExecutionStatus.SUCCEEDED);metrics.recordSchedulerSelected(SchedulerName.SUPPLY_DEMAND_ANALYSIS,result.size());}catch(RuntimeException e){histories.markFailed(historyId,e.getClass().getSimpleName(),clock.instant());metrics.recordSchedulerExecution(SchedulerName.SUPPLY_DEMAND_ANALYSIS,SchedulerExecutionStatus.FAILED);}}
}
