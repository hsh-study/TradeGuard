package seokhoon.trade.adapter.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import seokhoon.trade.application.port.in.EarlyMarketReportDataCompleteness;
import seokhoon.trade.application.port.in.EarlyMarketStrategyCandidateReport;
import seokhoon.trade.application.port.in.EarlyMarketStrategyDailyReport;
import seokhoon.trade.application.port.in.EarlyMarketStrategyGroupReport;
import seokhoon.trade.application.port.in.EarlyMarketStrategyPeriodReport;
import seokhoon.trade.application.port.in.LoadEarlyMarketStrategyPeriodReportUseCase;
import seokhoon.trade.application.port.in.LoadEarlyMarketStrategyReportUseCase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports/early-market")
public class EarlyMarketStrategyReportController {
    private final LoadEarlyMarketStrategyReportUseCase reportUseCase;
    private final LoadEarlyMarketStrategyPeriodReportUseCase periodReportUseCase;

    public EarlyMarketStrategyReportController(
            LoadEarlyMarketStrategyReportUseCase reportUseCase,
            LoadEarlyMarketStrategyPeriodReportUseCase periodReportUseCase
    ) {
        this.reportUseCase = reportUseCase;
        this.periodReportUseCase = periodReportUseCase;
    }

    @GetMapping("/daily")
    DailyReportResponse daily(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate tradeDate
    ) {
        return DailyReportResponse.from(reportUseCase.loadDailyReport(tradeDate));
    }

    @GetMapping("/period")
    EarlyMarketStrategyPeriodReport period(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to
    ) {
        return periodReportUseCase.loadPeriodReport(from, to);
    }

    public record DailyReportResponse(
            LocalDate tradeDate,
            int preScanCount,
            int entryCandidateCount,
            int performanceCapturedCount,
            int excludedFromPerformanceCount,
            BigDecimal averageMaxReturnRate,
            BigDecimal averageMaxDrawdownRate,
            EarlyMarketStrategyCandidateReport bestCandidate,
            EarlyMarketStrategyCandidateReport worstCandidate,
            Map<String, EarlyMarketStrategyGroupReport> bySignalType,
            Map<String, EarlyMarketStrategyGroupReport> byScoreBucket,
            Map<String, EarlyMarketStrategyGroupReport> byVwapBroken,
            Map<String, EarlyMarketStrategyGroupReport> byPreviousHighBreakout,
            Map<String, EarlyMarketStrategyGroupReport> byOpeningPriceHeld,
            Map<String, EarlyMarketStrategyGroupReport> byFollowUpDecision,
            EarlyMarketReportDataCompleteness dataCompleteness,
            List<EarlyMarketStrategyCandidateReport> candidates
    ) {
        static DailyReportResponse from(EarlyMarketStrategyDailyReport report) {
            return new DailyReportResponse(
                    report.tradeDate(),
                    report.preScanCount(),
                    report.entryCandidateCount(),
                    report.performanceCapturedCount(),
                    report.excludedFromPerformanceCount(),
                    report.averageMaxReturnRate(),
                    report.averageMaxDrawdownRate(),
                    report.bestCandidate(),
                    report.worstCandidate(),
                    report.bySignalType(),
                    report.byScoreBucket(),
                    report.byVwapBroken(),
                    report.byPreviousHighBreakout(),
                    report.byOpeningPriceHeld(),
                    report.byFollowUpDecision(),
                    report.dataCompleteness(),
                    report.candidates()
            );
        }
    }
}
