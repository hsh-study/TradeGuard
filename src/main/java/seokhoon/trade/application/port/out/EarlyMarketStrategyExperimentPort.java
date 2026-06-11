package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.strategy.EarlyMarketStrategyExperiment;

import java.util.List;
import java.util.Optional;

public interface EarlyMarketStrategyExperimentPort {
    EarlyMarketStrategyExperiment save(EarlyMarketStrategyExperiment experiment);

    Optional<EarlyMarketStrategyExperiment> findById(long id);

    List<EarlyMarketStrategyExperiment> findRecent(int limit);
}
