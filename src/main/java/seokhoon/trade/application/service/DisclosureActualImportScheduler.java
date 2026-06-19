package seokhoon.trade.application.service;

import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import seokhoon.trade.application.port.in.ImportDisclosureActualEvidenceUseCase;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.config.DisclosureActualProviderProperties;
import seokhoon.trade.domain.research.DisclosureEvidenceImportStatus;
import seokhoon.trade.domain.scheduler.*;

import java.time.*;
import java.util.*;

@Component
public class DisclosureActualImportScheduler {
    private final ImportDisclosureActualEvidenceUseCase useCase;
    private final MarketCalendarPort calendar;
    private final SchedulerExecutionHistoryPort histories;
    private final OperationalMetricsPort metrics;
    private final CorrelationIdProvider correlations;
    private final DisclosureActualProviderProperties properties;
    private final Clock clock;

    @Autowired
    public DisclosureActualImportScheduler(ImportDisclosureActualEvidenceUseCase useCase,
            MarketCalendarPort calendar, SchedulerExecutionHistoryPort histories,
            OperationalMetricsPort metrics, CorrelationIdProvider correlations,
            DisclosureActualProviderProperties properties) {
        this(useCase,calendar,histories,metrics,correlations,properties,Clock.system(ZoneId.of("Asia/Seoul")));
    }
    DisclosureActualImportScheduler(ImportDisclosureActualEvidenceUseCase useCase,
            MarketCalendarPort calendar,SchedulerExecutionHistoryPort histories,
            OperationalMetricsPort metrics,CorrelationIdProvider correlations,
            DisclosureActualProviderProperties properties,Clock clock) {
        this.useCase=useCase;this.calendar=calendar;this.histories=histories;this.metrics=metrics;
        this.correlations=correlations;this.properties=properties;this.clock=clock;
    }

    @Scheduled(cron="0 35 7 * * MON-FRI",zone="Asia/Seoul")
    public void importDisclosures(){String id=correlations.newCorrelationId();MDC.put("correlationId",id);try{execute(id);}finally{MDC.remove("correlationId");}}

    void execute(String id) {
        LocalDate date=LocalDate.now(clock);
        if(!calendar.isTradingDay(date)){skip(date,"NON_TRADING_DAY",id);return;}
        if(!properties.isEnabled()){skip(date,"PROVIDER_DISABLED",id);return;}
        if(!properties.isAutoRun()){skip(date,"AUTO_RUN_DISABLED",id);return;}
        long historyId=histories.saveStarted(SchedulerName.DISCLOSURE_ACTUAL_IMPORT,date,id,clock.instant());
        metrics.recordSchedulerExecution(SchedulerName.DISCLOSURE_ACTUAL_IMPORT,SchedulerExecutionStatus.STARTED);
        try {
            var results=new ArrayList<>(useCase.importWatchlist(date));results.addAll(useCase.importHoldings(date));
            int imported=results.stream().mapToInt(v->v.importedCount()).sum();
            boolean failed=results.stream().anyMatch(v->v.status()==DisclosureEvidenceImportStatus.FAILED);
            if(failed){histories.markFailed(historyId,"ONE_OR_MORE_DISCLOSURE_IMPORTS_FAILED",clock.instant());
                metrics.recordSchedulerExecution(SchedulerName.DISCLOSURE_ACTUAL_IMPORT,SchedulerExecutionStatus.FAILED);}
            else{histories.markSucceeded(historyId,results.size(),imported,false,clock.instant());
                metrics.recordSchedulerExecution(SchedulerName.DISCLOSURE_ACTUAL_IMPORT,SchedulerExecutionStatus.SUCCEEDED);
                metrics.recordSchedulerSelected(SchedulerName.DISCLOSURE_ACTUAL_IMPORT,imported);}
        }catch(RuntimeException e){histories.markFailed(historyId,e.getClass().getSimpleName(),clock.instant());
            metrics.recordSchedulerExecution(SchedulerName.DISCLOSURE_ACTUAL_IMPORT,SchedulerExecutionStatus.FAILED);}
    }
    private void skip(LocalDate date,String reason,String id){histories.markSkipped(SchedulerName.DISCLOSURE_ACTUAL_IMPORT,date,reason,id,clock.instant());
        metrics.recordSchedulerExecution(SchedulerName.DISCLOSURE_ACTUAL_IMPORT,SchedulerExecutionStatus.SKIPPED);}
}
