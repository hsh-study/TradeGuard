package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.research.PaperTradingReportResult;

import java.time.LocalDate;
import java.util.List;

public interface GeneratePaperTradingReportUseCase {
    PaperTradingReportView generateDailyReport(LocalDate tradeDate);
    PaperTradingReportView getRun(long runId);
    List<PaperTradingReportResult> getResults(long runId);
    PaperTradingReportView getLatestByTradeDate(LocalDate tradeDate);
}
