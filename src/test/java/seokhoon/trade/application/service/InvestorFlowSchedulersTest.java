package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.*;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.config.InvestorFlowProperties;
import seokhoon.trade.domain.scheduler.*;
import java.time.*;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class InvestorFlowSchedulersTest {
    @Test void importSchedulerSkipsNonTradingDayWithoutProviderUseCase(){var useCase=mock(ImportInvestorFlowsUseCase.class);var calendar=mock(MarketCalendarPort.class);when(calendar.isTradingDay(any())).thenReturn(false);var history=new History();var properties=new InvestorFlowProperties();properties.setImportAutoRun(true);var scheduler=new InvestorFlowImportScheduler(useCase,calendar,history,OperationalMetricsPort.noop(),ids(),properties,clock());scheduler.importFlows();assertThat(history.status).isEqualTo(SchedulerExecutionStatus.SKIPPED);assertThat(history.reason).isEqualTo("NON_TRADING_DAY");verifyNoInteractions(useCase);}
    @Test void analysisSchedulerSkipsNonTradingDay(){var useCase=mock(AnalyzeSupplyDemandUseCase.class);var calendar=mock(MarketCalendarPort.class);when(calendar.isTradingDay(any())).thenReturn(false);var history=new History();var scheduler=new SupplyDemandAnalysisScheduler(useCase,calendar,history,OperationalMetricsPort.noop(),ids(),clock());scheduler.analyze();assertThat(history.status).isEqualTo(SchedulerExecutionStatus.SKIPPED);verifyNoInteractions(useCase);}
    private static Clock clock(){return Clock.fixed(Instant.parse("2026-06-14T00:00:00Z"),ZoneId.of("Asia/Seoul"));}
    private static CorrelationIdProvider ids(){return new CorrelationIdProvider(){public String currentCorrelationId(){return "corr";}public String newCorrelationId(){return "corr";}};}
    private static class History implements SchedulerExecutionHistoryPort{SchedulerExecutionStatus status;String reason;public long saveStarted(SchedulerName n,LocalDate d,String c,Instant a){status=SchedulerExecutionStatus.STARTED;return 1;}public void markSucceeded(long id,int c,int s,boolean sent,Instant a){status=SchedulerExecutionStatus.SUCCEEDED;}public void markFailed(long id,String r,Instant a){status=SchedulerExecutionStatus.FAILED;reason=r;}public void markSkipped(SchedulerName n,LocalDate d,String r,String c,Instant a){status=SchedulerExecutionStatus.SKIPPED;reason=r;}public List<SchedulerExecutionHistoryRecord> find(LocalDate d,SchedulerName n,SchedulerExecutionStatus s){return List.of();}}
}
