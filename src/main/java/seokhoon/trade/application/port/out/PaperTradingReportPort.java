package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.research.PaperTradingReportResult;
import seokhoon.trade.domain.research.PaperTradingReportRun;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PaperTradingReportPort {
    PaperTradingReportRun saveRun(PaperTradingReportRun run);
    List<PaperTradingReportResult> saveResults(List<PaperTradingReportResult> results);
    Optional<PaperTradingReportRun> findRun(long runId);
    Optional<PaperTradingReportRun> findLatestRun(LocalDate tradeDate);
    List<PaperTradingReportResult> findResults(long runId);
}
