package seokhoon.trade.application.port.in;

import java.util.List;

public interface CompareEarlyMarketStrategyExperimentsUseCase {
    EarlyMarketStrategyExperimentComparison compare(List<Long> experimentIds);
}
