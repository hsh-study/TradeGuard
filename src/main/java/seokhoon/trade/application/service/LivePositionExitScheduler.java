package seokhoon.trade.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.in.LiveTradingUseCases.EvaluateLivePositionExitUseCase;
import seokhoon.trade.config.LiveTradingProperties;

@Component
public class LivePositionExitScheduler {
    private static final Logger log=LoggerFactory.getLogger(LivePositionExitScheduler.class);
    private final EvaluateLivePositionExitUseCase useCase;
    private final LiveTradingProperties properties;
    public LivePositionExitScheduler(EvaluateLivePositionExitUseCase useCase,LiveTradingProperties properties){this.useCase=useCase;this.properties=properties;}

    @Scheduled(cron="${tradeguard.live-trading.exit-monitor-cron:0 * * * * MON-FRI}",zone="Asia/Seoul")
    public void monitor(){
        if(!properties.isLiveTradingEnabled()||!properties.isKisTradingEnabled())return;
        try{
            var result=useCase.evaluate();
            log.atInfo().addKeyValue("schedulerName","LIVE_POSITION_EXIT_MONITOR")
                    .addKeyValue("evaluatedCount",result.size())
                    .log("Live position exit monitor completed");
        }catch(RuntimeException exception){
            log.atWarn().addKeyValue("schedulerName","LIVE_POSITION_EXIT_MONITOR")
                    .addKeyValue("errorType",exception.getClass().getSimpleName())
                    .log("Live position exit monitor skipped or failed");
        }
    }
}
