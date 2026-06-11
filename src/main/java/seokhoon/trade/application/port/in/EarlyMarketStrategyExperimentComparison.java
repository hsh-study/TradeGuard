package seokhoon.trade.application.port.in;

import java.time.Instant;
import java.util.List;

public record EarlyMarketStrategyExperimentComparison(
        List<Long> experimentIds,
        Instant comparedAt,
        List<EarlyMarketStrategyExperimentComparisonItem> experiments,
        EarlyMarketStrategyExperimentComparisonItem bestByWinRate,
        EarlyMarketStrategyExperimentComparisonItem bestByAverageMaxReturnRate,
        EarlyMarketStrategyExperimentComparisonItem bestByAverageMaxDrawdownRate,
        List<String> notes
) {
}
