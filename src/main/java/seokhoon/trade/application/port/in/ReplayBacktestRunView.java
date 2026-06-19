package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.research.ReplayBacktestRun;

import java.math.BigDecimal;
import java.util.List;

public record ReplayBacktestRunView(
        ReplayBacktestRun run,
        BigDecimal winRate,
        BigDecimal medianReturnRate,
        BigDecimal averageWinScore,
        BigDecimal averageLossScore,
        List<ReplayBacktestBreakdown> performanceByReason,
        List<ReplayBacktestBreakdown> performanceByWarning
) {
}
