package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.*;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.config.PaperTradingReportProperties;
import seokhoon.trade.domain.research.PaperTradingReportResult;
import seokhoon.trade.domain.scheduler.*;

import java.time.*;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PaperTradingReportSchedulerTest {
    @Test void skipsNonTradingDayWithoutGeneratingReport() {
        CountingUseCase useCase=new CountingUseCase(); RecordingHistory history=new RecordingHistory();
        PaperTradingReportProperties properties=new PaperTradingReportProperties(); properties.setAutoRun(true);
        PaperTradingReportScheduler scheduler=new PaperTradingReportScheduler(useCase,date->false,history,
                OperationalMetricsPort.noop(),new Correlation(),properties,
                Clock.fixed(Instant.parse("2026-06-14T07:10:00Z"),ZoneId.of("Asia/Seoul")));
        scheduler.generateDailyReport();
        assertThat(useCase.count).isZero(); assertThat(history.name).isEqualTo(SchedulerName.PAPER_TRADING_DAILY_REPORT);
        assertThat(history.reason).isEqualTo("NON_TRADING_DAY");
    }
    private static class CountingUseCase implements GeneratePaperTradingReportUseCase {
        int count; public PaperTradingReportView generateDailyReport(LocalDate d){count++;return null;}
        public PaperTradingReportView getRun(long id){return null;} public List<PaperTradingReportResult> getResults(long id){return List.of();}
        public PaperTradingReportView getLatestByTradeDate(LocalDate d){return null;}
    }
    private static class Correlation implements CorrelationIdProvider { public String currentCorrelationId(){return "paper";} public String newCorrelationId(){return "paper";} }
    private static class RecordingHistory implements SchedulerExecutionHistoryPort {
        SchedulerName name; String reason; public long saveStarted(SchedulerName n,LocalDate d,String c,Instant i){return 1;}
        public void markSucceeded(long id,int s,int selected,boolean sent,Instant i){} public void markFailed(long id,String r,Instant i){}
        public void markSkipped(SchedulerName n,LocalDate d,String r,String c,Instant i){name=n;reason=r;}
        public List<SchedulerExecutionHistoryRecord> find(LocalDate d,SchedulerName n,SchedulerExecutionStatus s){return List.of();}
    }
}
