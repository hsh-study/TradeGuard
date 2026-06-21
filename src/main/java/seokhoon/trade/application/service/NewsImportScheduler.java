package seokhoon.trade.application.service;
import org.springframework.scheduling.annotation.Scheduled; import org.springframework.stereotype.Component; import org.springframework.beans.factory.annotation.Autowired;
import seokhoon.trade.application.port.in.*; import seokhoon.trade.application.port.out.*; import seokhoon.trade.config.NaverNewsProperties;
import seokhoon.trade.domain.scheduler.*; import java.time.*; import java.util.UUID;
@Component public class NewsImportScheduler {
    private final NewsUseCase news;private final NaverNewsProperties properties;private final LoadMarketCalendarUseCase calendar;
    private final SchedulerExecutionHistoryPort histories;private final OperationalMetricsPort metrics;private final Clock clock;
    @Autowired public NewsImportScheduler(NewsUseCase n,NaverNewsProperties p,LoadMarketCalendarUseCase c,SchedulerExecutionHistoryPort h,OperationalMetricsPort m){this(n,p,c,h,m,Clock.system(ZoneId.of("Asia/Seoul")));}
    NewsImportScheduler(NewsUseCase n,NaverNewsProperties p,LoadMarketCalendarUseCase c,SchedulerExecutionHistoryPort h,OperationalMetricsPort m,Clock clock){news=n;properties=p;calendar=c;histories=h;metrics=m;this.clock=clock;}
    @Scheduled(cron="${tradeguard.research.news.naver.auto-run-cron:0 20 7,12,16 * * MON-FRI}",zone="Asia/Seoul") public void scheduled(){execute(UUID.randomUUID().toString());}
    void execute(String id){LocalDate today=LocalDate.now(clock);if(!properties.isProviderEnabled()){skip(today,"NEWS_PROVIDER_DISABLED",id);return;}if(!properties.isAutoRun()){skip(today,"NEWS_AUTO_RUN_DISABLED",id);return;}if(!calendar.load(today).tradingDay()){skip(today,"NON_TRADING_DAY",id);return;}long history=histories.saveStarted(SchedulerName.NEWS_IMPORT,today,id,clock.instant());metrics.recordSchedulerExecution(SchedulerName.NEWS_IMPORT,SchedulerExecutionStatus.STARTED);try{var results=news.importWatchlist();int saved=results.stream().mapToInt(NewsUseCase.ImportResult::savedCount).sum();histories.markSucceeded(history,results.size(),saved,false,clock.instant());metrics.recordSchedulerExecution(SchedulerName.NEWS_IMPORT,SchedulerExecutionStatus.SUCCEEDED);}catch(RuntimeException e){histories.markFailed(history,"NEWS_IMPORT_FAILED",clock.instant());metrics.recordSchedulerExecution(SchedulerName.NEWS_IMPORT,SchedulerExecutionStatus.FAILED);}}
    private void skip(LocalDate d,String reason,String id){histories.markSkipped(SchedulerName.NEWS_IMPORT,d,reason,id,clock.instant());metrics.recordSchedulerExecution(SchedulerName.NEWS_IMPORT,SchedulerExecutionStatus.SKIPPED);}
}
