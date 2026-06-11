package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.strategy.EarlyMarketStrategyExperiment;

import java.util.List;

public record EarlyMarketStrategyBacktestResult(
        EarlyMarketStrategyExperiment experiment,
        EarlyMarketStrategyBacktestPeriodSummary periodReportSummary,
        List<String> warnings
) {
}
