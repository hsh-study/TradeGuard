package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.strategy.EarlyMarketStrategyExperiment;

import java.util.List;

public interface LoadEarlyMarketStrategyExperimentsUseCase {
    EarlyMarketStrategyExperiment findById(long id);

    List<EarlyMarketStrategyExperiment> findRecent(int limit);
}
