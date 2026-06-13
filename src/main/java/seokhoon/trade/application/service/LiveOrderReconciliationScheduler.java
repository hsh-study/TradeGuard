package seokhoon.trade.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.in.LiveTradingUseCases.ReconcileLiveOrdersUseCase;
import seokhoon.trade.config.LiveTradingProperties;

@Component
public class LiveOrderReconciliationScheduler {
    private static final Logger log=LoggerFactory.getLogger(LiveOrderReconciliationScheduler.class);
    private final ReconcileLiveOrdersUseCase useCase;
    private final LiveTradingProperties properties;
    public LiveOrderReconciliationScheduler(ReconcileLiveOrdersUseCase useCase,LiveTradingProperties properties){this.useCase=useCase;this.properties=properties;}
    @Scheduled(cron="${tradeguard.live-trading.reconciliation-cron:30 * * * * MON-FRI}",zone="Asia/Seoul")
    public void reconcile(){
        if(!properties.isKisTradingEnabled())return;
        try{
            int count=useCase.reconcile();
            log.atInfo().addKeyValue("schedulerName","LIVE_ORDER_RECONCILIATION")
                    .addKeyValue("fillCount",count).log("Live order reconciliation completed");
        }catch(RuntimeException exception){
            log.atWarn().addKeyValue("schedulerName","LIVE_ORDER_RECONCILIATION")
                    .addKeyValue("errorType",exception.getClass().getSimpleName())
                    .log("Live order reconciliation skipped or failed");
        }
    }
}
