package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.research.PaperTradingReportResult;
import seokhoon.trade.domain.research.PaperTradingReportRun;

import java.math.BigDecimal;
import java.util.List;

public record PaperTradingReportView(
        PaperTradingReportRun run, BigDecimal winRate, int dataInsufficientCount,
        List<PaperTradingPerformanceBreakdown> performanceByStrategy,
        List<PaperTradingPerformanceBreakdown> performanceByReason,
        List<PaperTradingPerformanceBreakdown> performanceByWarning,
        List<PaperTradingReportResult> topWinners,
        List<PaperTradingReportResult> topLosers
) { }
